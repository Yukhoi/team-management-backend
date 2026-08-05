package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.client.TeamServiceClient;
import com.yukai.team.matchservice.client.TournamentClient;
import com.yukai.team.matchservice.client.dto.InternalTeamInfo;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMappingResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.UpsertFlaTeamMappingRequest;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeam;
import com.yukai.team.matchservice.opponentanalysis.entity.FlaTeamMapping;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaMappingConflictException;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaChampionnatRepository;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaTeamMappingRepository;
import com.yukai.team.matchservice.opponentanalysis.repository.FlaTeamRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FlaTeamMappingServiceImpl implements FlaTeamMappingService {

    private final FlaTeamMappingRepository flaTeamMappingRepository;
    private final FlaChampionnatRepository flaChampionnatRepository;
    private final FlaTeamRepository flaTeamRepository;
    private final TeamServiceClient teamServiceClient;
    private final TournamentClient tournamentClient;

    public FlaTeamMappingServiceImpl(
            FlaTeamMappingRepository flaTeamMappingRepository,
            FlaChampionnatRepository flaChampionnatRepository,
            FlaTeamRepository flaTeamRepository,
            TeamServiceClient teamServiceClient,
            TournamentClient tournamentClient
    ) {
        this.flaTeamMappingRepository = flaTeamMappingRepository;
        this.flaChampionnatRepository = flaChampionnatRepository;
        this.flaTeamRepository = flaTeamRepository;
        this.teamServiceClient = teamServiceClient;
        this.tournamentClient = tournamentClient;
    }

    @Override
    @Transactional
    public FlaTeamMappingResponse upsertMapping(Long tournamentId, Long teamId, UpsertFlaTeamMappingRequest request) {
        validatePositive(tournamentId, "tournamentId");
        validatePositive(teamId, "teamId");
        validateTournament(tournamentId);
        InternalTeamInfo internalTeam = validateTeam(teamId);
        validateFlaChampionnat(request.getFlaChampionnatId(), request.getFlaSaisonId());
        FlaTeam flaTeam = findFlaTeam(request);
        validateExternalTeamAvailable(tournamentId, teamId, request);

        FlaTeamMapping mapping = flaTeamMappingRepository
                .findByInternalTournamentIdAndInternalTeamId(tournamentId, teamId)
                .orElseGet(FlaTeamMapping::new);
        mapping.setInternalTournamentId(tournamentId);
        mapping.setInternalTeamId(teamId);
        mapping.setFlaChampionnatId(request.getFlaChampionnatId());
        mapping.setFlaSaisonId(request.getFlaSaisonId());
        mapping.setFlaTeamId(request.getFlaTeamId());
        mapping.setFlaTeamName(flaTeam.getTeamName());

        FlaTeamMapping saved = flaTeamMappingRepository.save(mapping);
        return toResponse(saved, internalTeam.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FlaTeamMappingResponse> getMappings(Long tournamentId) {
        validatePositive(tournamentId, "tournamentId");
        validateTournament(tournamentId);
        List<FlaTeamMapping> mappings = flaTeamMappingRepository.findByInternalTournamentIdOrderByInternalTeamIdAsc(tournamentId);
        Map<Long, InternalTeamInfo> teamsById = mappings.stream()
                .map(FlaTeamMapping::getInternalTeamId)
                .distinct()
                .map(this::validateTeam)
                .collect(Collectors.toMap(InternalTeamInfo::getId, Function.identity()));

        return mappings.stream()
                .map(mapping -> toResponse(mapping, resolveTeamName(teamsById.get(mapping.getInternalTeamId()))))
                .sorted(Comparator.comparing(
                        FlaTeamMappingResponse::getInternalTeamName,
                        Comparator.nullsLast(String::compareToIgnoreCase)
                ).thenComparing(FlaTeamMappingResponse::getInternalTeamId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FlaTeamMappingResponse getMapping(Long tournamentId, Long teamId) {
        validatePositive(tournamentId, "tournamentId");
        validatePositive(teamId, "teamId");
        validateTournament(tournamentId);
        InternalTeamInfo internalTeam = validateTeam(teamId);
        FlaTeamMapping mapping = flaTeamMappingRepository
                .findByInternalTournamentIdAndInternalTeamId(tournamentId, teamId)
                .orElseThrow(() -> new EntityNotFoundException("FLA team mapping not found"));
        return toResponse(mapping, internalTeam.getName());
    }

    @Override
    @Transactional
    public void deleteMapping(Long tournamentId, Long teamId) {
        validatePositive(tournamentId, "tournamentId");
        validatePositive(teamId, "teamId");
        validateTournament(tournamentId);
        validateTeam(teamId);
        FlaTeamMapping mapping = flaTeamMappingRepository
                .findByInternalTournamentIdAndInternalTeamId(tournamentId, teamId)
                .orElseThrow(() -> new EntityNotFoundException("FLA team mapping not found"));
        flaTeamMappingRepository.delete(mapping);
    }

    private void validateTournament(Long tournamentId) {
        try {
            if (tournamentClient.getTournamentSnapshot(tournamentId) == null) {
                throw new EntityNotFoundException("Tournament not found");
            }
        } catch (FeignException.NotFound exception) {
            throw new EntityNotFoundException("Tournament not found");
        }
    }

    private InternalTeamInfo validateTeam(Long teamId) {
        InternalTeamInfo team = teamServiceClient.getTeam(teamId);
        if (team == null || team.getId() == null) {
            throw new EntityNotFoundException("Team not found");
        }
        return team;
    }

    private void validateFlaChampionnat(Long flaChampionnatId, Long flaSaisonId) {
        flaChampionnatRepository.findByChampionnatIdAndSaisonId(flaChampionnatId, flaSaisonId)
                .orElseThrow(() -> new EntityNotFoundException("FLA championnat not found"));
    }

    private FlaTeam findFlaTeam(UpsertFlaTeamMappingRequest request) {
        return flaTeamRepository.findByChampionnatIdAndSaisonIdAndFlaTeamId(
                        request.getFlaChampionnatId(),
                        request.getFlaSaisonId(),
                        request.getFlaTeamId()
                )
                .orElseThrow(() -> new EntityNotFoundException("FLA team not found"));
    }

    private void validateExternalTeamAvailable(
            Long tournamentId,
            Long teamId,
            UpsertFlaTeamMappingRequest request
    ) {
        boolean occupied = flaTeamMappingRepository
                .existsByInternalTournamentIdAndFlaChampionnatIdAndFlaSaisonIdAndFlaTeamIdAndInternalTeamIdNot(
                        tournamentId,
                        request.getFlaChampionnatId(),
                        request.getFlaSaisonId(),
                        request.getFlaTeamId(),
                        teamId
                );
        if (occupied) {
            throw new FlaMappingConflictException("FLA team is already mapped in this tournament");
        }
    }

    private FlaTeamMappingResponse toResponse(FlaTeamMapping mapping, String internalTeamName) {
        return new FlaTeamMappingResponse(
                mapping.getId(),
                mapping.getInternalTournamentId(),
                mapping.getInternalTeamId(),
                internalTeamName,
                mapping.getFlaChampionnatId(),
                mapping.getFlaSaisonId(),
                mapping.getFlaTeamId(),
                mapping.getFlaTeamName(),
                mapping.getCreatedAt(),
                mapping.getUpdatedAt(),
                mapping.getVersion()
        );
    }

    private String resolveTeamName(InternalTeamInfo team) {
        return team == null ? null : team.getName();
    }

    private void validatePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
