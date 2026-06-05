package com.yukai.team.tournamentservice.service.impl;

import com.yukai.team.tournamentservice.dto.event.TournamentCancelledEvent;
import com.yukai.team.tournamentservice.dto.event.TournamentCreatedEvent;
import com.yukai.team.tournamentservice.dto.event.TournamentFinishedEvent;
import com.yukai.team.tournamentservice.dto.event.TournamentUpdatedEvent;
import com.yukai.team.tournamentservice.dto.request.CreateTournamentRequest;
import com.yukai.team.tournamentservice.dto.request.UpdateTournamentRequest;
import com.yukai.team.tournamentservice.dto.response.PageResponse;
import com.yukai.team.tournamentservice.dto.response.TournamentResponse;
import com.yukai.team.tournamentservice.dto.response.TournamentSnapshotResponse;
import com.yukai.team.tournamentservice.entity.Tournament;
import com.yukai.team.tournamentservice.entity.enums.TournamentStatus;
import com.yukai.team.tournamentservice.entity.enums.TournamentType;
import com.yukai.team.tournamentservice.exception.BusinessException;
import com.yukai.team.tournamentservice.exception.ResourceNotFoundException;
import com.yukai.team.tournamentservice.repository.TournamentRepository;
import com.yukai.team.tournamentservice.service.OutboxEventService;
import com.yukai.team.tournamentservice.service.TournamentService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class TournamentServiceImpl implements TournamentService {

    private static final String AGGREGATE_TYPE_TOURNAMENT = "tournament";

    private final TournamentRepository tournamentRepository;
    private final OutboxEventService outboxEventService;

    public TournamentServiceImpl(
            TournamentRepository tournamentRepository,
            OutboxEventService outboxEventService
    ) {
        this.tournamentRepository = tournamentRepository;
        this.outboxEventService = outboxEventService;
    }

    @Override
    @Transactional
    public TournamentResponse createTournament(CreateTournamentRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        validateTournamentDoesNotExist(request.getName(), request.getSeason());

        Tournament tournament = new Tournament();
        tournament.setName(request.getName().trim());
        tournament.setTournamentType(request.getTournamentType());
        tournament.setSeason(request.getSeason().trim());
        tournament.setStartDate(request.getStartDate());
        tournament.setEndDate(request.getEndDate());
        tournament.setOrganizer(trimToNull(request.getOrganizer()));
        tournament.setDescription(trimToNull(request.getDescription()));
        tournament.setStatus(TournamentStatus.ACTIVE);

        Tournament savedTournament = tournamentRepository.save(tournament);
        outboxEventService.saveEvent(
                AGGREGATE_TYPE_TOURNAMENT,
                savedTournament.getId(),
                "tournament.created",
                TournamentCreatedEvent.builder()
                        .tournamentId(savedTournament.getId())
                        .build()
        );
        return toResponse(savedTournament);
    }

    @Override
    @Transactional
    public TournamentResponse updateTournament(Long id, UpdateTournamentRequest request) {
        Tournament tournament = findTournament(id);
        validateTournamentActiveForUpdate(tournament);
        validateDateRange(request.getStartDate(), request.getEndDate());
        validateTournamentDoesNotExistForUpdate(tournament, request.getName(), request.getSeason());

        tournament.setName(request.getName().trim());
        tournament.setTournamentType(request.getTournamentType());
        tournament.setSeason(request.getSeason().trim());
        tournament.setStartDate(request.getStartDate());
        tournament.setEndDate(request.getEndDate());
        tournament.setOrganizer(trimToNull(request.getOrganizer()));
        tournament.setDescription(trimToNull(request.getDescription()));

        Tournament savedTournament = tournamentRepository.save(tournament);
        outboxEventService.saveEvent(
                AGGREGATE_TYPE_TOURNAMENT,
                savedTournament.getId(),
                "tournament.updated",
                TournamentUpdatedEvent.builder()
                        .tournamentId(savedTournament.getId())
                        .build()
        );
        return toResponse(savedTournament);
    }

    @Override
    @Transactional(readOnly = true)
    public TournamentResponse getTournament(Long id) {
        return toResponse(findTournament(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TournamentResponse> getTournaments(
            String season,
            TournamentStatus status,
            TournamentType tournamentType,
            Pageable pageable
    ) {
        Page<Tournament> tournamentPage = tournamentRepository.findAll(
                buildSpecification(season, status, tournamentType),
                pageable
        );
        List<TournamentResponse> content = tournamentPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.from(tournamentPage, content);
    }

    @Override
    @Transactional
    public TournamentResponse finishTournament(Long id) {
        Tournament tournament = findTournament(id);
        if (tournament.getStatus() != TournamentStatus.ACTIVE) {
            throw new BusinessException("Only ACTIVE tournament can be finished");
        }

        tournament.setStatus(TournamentStatus.FINISHED);
        Tournament savedTournament = tournamentRepository.save(tournament);
        outboxEventService.saveEvent(
                AGGREGATE_TYPE_TOURNAMENT,
                savedTournament.getId(),
                "tournament.finished",
                TournamentFinishedEvent.builder()
                        .tournamentId(savedTournament.getId())
                        .build()
        );
        return toResponse(savedTournament);
    }

    @Override
    @Transactional
    public TournamentResponse cancelTournament(Long id) {
        Tournament tournament = findTournament(id);
        if (tournament.getStatus() != TournamentStatus.ACTIVE) {
            throw new BusinessException("Only ACTIVE tournament can be cancelled");
        }

        tournament.setStatus(TournamentStatus.CANCELLED);
        Tournament savedTournament = tournamentRepository.save(tournament);
        outboxEventService.saveEvent(
                AGGREGATE_TYPE_TOURNAMENT,
                savedTournament.getId(),
                "tournament.cancelled",
                TournamentCancelledEvent.builder()
                        .tournamentId(savedTournament.getId())
                        .build()
        );
        return toResponse(savedTournament);
    }

    @Override
    @Transactional(readOnly = true)
    public TournamentSnapshotResponse getTournamentSnapshot(Long id) {
        return toSnapshotResponse(findTournament(id));
    }

    private Specification<Tournament> buildSpecification(
            String season,
            TournamentStatus status,
            TournamentType tournamentType
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            String trimmedSeason = trimToNull(season);
            if (trimmedSeason != null) {
                predicates.add(criteriaBuilder.equal(root.get("season"), trimmedSeason));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (tournamentType != null) {
                predicates.add(criteriaBuilder.equal(root.get("tournamentType"), tournamentType));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validateTournamentActiveForUpdate(Tournament tournament) {
        if (tournament.getStatus() == TournamentStatus.FINISHED) {
            throw new BusinessException("Cannot update finished tournament");
        }
        if (tournament.getStatus() == TournamentStatus.CANCELLED) {
            throw new BusinessException("Cannot update cancelled tournament");
        }
    }

    private void validateTournamentDoesNotExist(String name, String season) {
        if (tournamentRepository.existsByNameAndSeason(name.trim(), season.trim())) {
            throw new BusinessException("Tournament already exists");
        }
    }

    private void validateTournamentDoesNotExistForUpdate(Tournament tournament, String name, String season) {
        String normalizedName = name.trim();
        String normalizedSeason = season.trim();
        if (Objects.equals(tournament.getName(), normalizedName)
                && Objects.equals(tournament.getSeason(), normalizedSeason)) {
            return;
        }
        validateTournamentDoesNotExist(normalizedName, normalizedSeason);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessException("Invalid tournament date range");
        }
    }

    private Tournament findTournament(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found: " + id));
    }

    private TournamentResponse toResponse(Tournament tournament) {
        return TournamentResponse.builder()
                .id(tournament.getId())
                .name(tournament.getName())
                .tournamentType(tournament.getTournamentType())
                .season(tournament.getSeason())
                .startDate(tournament.getStartDate())
                .endDate(tournament.getEndDate())
                .organizer(tournament.getOrganizer())
                .description(tournament.getDescription())
                .status(tournament.getStatus())
                .createdAt(tournament.getCreatedAt())
                .updatedAt(tournament.getUpdatedAt())
                .version(tournament.getVersion())
                .build();
    }

    private TournamentSnapshotResponse toSnapshotResponse(Tournament tournament) {
        return TournamentSnapshotResponse.builder()
                .id(tournament.getId())
                .name(tournament.getName())
                .season(tournament.getSeason())
                .tournamentType(tournament.getTournamentType())
                .status(tournament.getStatus())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
