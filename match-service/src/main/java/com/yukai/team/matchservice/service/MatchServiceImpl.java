package com.yukai.team.matchservice.service;

import com.yukai.team.matchservice.client.TeamServiceClient;
import com.yukai.team.matchservice.client.TournamentClient;
import com.yukai.team.matchservice.client.dto.CreateOpponentTeamResponse;
import com.yukai.team.matchservice.client.dto.InternalTeamInfo;
import com.yukai.team.matchservice.client.dto.ValidateMatchTeamsResponse;
import com.yukai.team.matchservice.client.dto.ValidatePlayersResponse;
import com.yukai.team.matchservice.dto.CreateMatchRequest;
import com.yukai.team.matchservice.dto.MatchAppearanceResponse;
import com.yukai.team.matchservice.dto.MatchResponse;
import com.yukai.team.matchservice.dto.PageResponse;
import com.yukai.team.matchservice.dto.PlayerAppearanceRequest;
import com.yukai.team.matchservice.dto.ReplaceAppearanceRequest;
import com.yukai.team.matchservice.dto.UpdateMatchResultRequest;
import com.yukai.team.matchservice.dto.response.TournamentSnapshotResponse;
import com.yukai.team.matchservice.entity.MatchAppearance;
import com.yukai.team.matchservice.entity.MatchInfo;
import com.yukai.team.matchservice.entity.MatchResultHistory;
import com.yukai.team.matchservice.entity.MatchStatus;
import com.yukai.team.matchservice.exception.BusinessException;
import com.yukai.team.matchservice.outbox.MatchEventPayloadFactory;
import com.yukai.team.matchservice.repository.MatchAppearanceRepository;
import com.yukai.team.matchservice.repository.MatchInfoRepository;
import com.yukai.team.matchservice.repository.MatchResultHistoryRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MatchServiceImpl implements MatchService {

    private static final String AGGREGATE_TYPE_MATCH = "match";

    private final MatchInfoRepository matchInfoRepository;
    private final MatchAppearanceRepository matchAppearanceRepository;
    private final MatchResultHistoryRepository matchResultHistoryRepository;
    private final OutboxEventService outboxEventService;
    private final TeamServiceClient teamServiceClient;
    private final TournamentClient tournamentClient;

    public MatchServiceImpl(
            MatchInfoRepository matchInfoRepository,
            MatchAppearanceRepository matchAppearanceRepository,
            MatchResultHistoryRepository matchResultHistoryRepository,
            OutboxEventService outboxEventService,
            TeamServiceClient teamServiceClient,
            TournamentClient tournamentClient
    ) {
        this.matchInfoRepository = matchInfoRepository;
        this.matchAppearanceRepository = matchAppearanceRepository;
        this.matchResultHistoryRepository = matchResultHistoryRepository;
        this.outboxEventService = outboxEventService;
        this.teamServiceClient = teamServiceClient;
        this.tournamentClient = tournamentClient;
    }

    @Override
    @Transactional
    public MatchResponse createMatch(CreateMatchRequest request) {
        if (request.getOpponentTeamId() != null && request.getOurTeamId().equals(request.getOpponentTeamId())) {
            throw new IllegalArgumentException("ourTeamId and opponentTeamId must be different");
        }

        Long opponentTeamId = resolveOpponentTeamId(request);
        ValidateMatchTeamsResponse teams = validateMatchTeams(request.getOurTeamId(), opponentTeamId);
        TournamentSnapshotResponse tournament = resolveTournamentSnapshot(request.getTournamentId());
        InternalTeamInfo ourTeam = teams.getOurTeam();
        InternalTeamInfo opponentTeam = teams.getOpponentTeam();

        MatchInfo match = new MatchInfo();
        match.setTournamentId(tournament.getId());
        match.setTournamentNameSnapshot(tournament.getName());
        match.setSeasonSnapshot(tournament.getSeason());
        match.setOurTeamId(ourTeam.getId());
        match.setOurTeamNameSnapshot(ourTeam.getName());
        match.setOpponentTeamId(opponentTeam.getId());
        match.setOpponentTeamNameSnapshot(opponentTeam.getName());
        match.setMatchTime(request.getMatchTime());
        match.setHomeAway(request.getHomeAway());
        match.setVenue(request.getVenue());
        match.setRoundStage(request.getRoundStage());
        match.setMatchStatus(MatchStatus.SCHEDULED);
        match.setOurScore(0);
        match.setOpponentScore(0);
        match.setFinished(false);

        MatchInfo savedMatch = matchInfoRepository.save(match);
        outboxEventService.saveEvent(
                AGGREGATE_TYPE_MATCH,
                savedMatch.getId(),
                "match.created",
                MatchEventPayloadFactory.matchCreated(savedMatch)
        );
        return toResponse(savedMatch);
    }

    private TournamentSnapshotResponse resolveTournamentSnapshot(Long tournamentId) {
        TournamentSnapshotResponse tournament;
        try {
            tournament = tournamentClient.getTournamentSnapshot(tournamentId);
        } catch (FeignException ex) {
            if (ex.status() == 404) {
                throw new EntityNotFoundException("Tournament not found");
            }
            throw new IllegalStateException("Failed to get tournament snapshot from tournament-service", ex);
        }

        if (tournament == null || tournament.getId() == null) {
            throw new IllegalStateException("Failed to get tournament snapshot from tournament-service");
        }
        if (!"ACTIVE".equals(tournament.getStatus())) {
            throw new BusinessException("Tournament is not active");
        }
        return tournament;
    }

    private Long resolveOpponentTeamId(CreateMatchRequest request) {
        if (request.getOpponentTeamId() != null) {
            return request.getOpponentTeamId();
        }

        String opponentTeamName = trimToNull(request.getOpponentTeamName());
        if (opponentTeamName == null) {
            throw new IllegalArgumentException("opponentTeamName must not be blank when opponentTeamId is null");
        }

        CreateOpponentTeamResponse response = teamServiceClient.createOpponentTeam(opponentTeamName);
        if (response == null || response.getTeamId() == null) {
            throw new IllegalStateException("Failed to create opponent team from team-service");
        }
        return response.getTeamId();
    }

    private ValidateMatchTeamsResponse validateMatchTeams(Long ourTeamId, Long opponentTeamId) {
        ValidateMatchTeamsResponse response = teamServiceClient.validateMatchTeams(ourTeamId, opponentTeamId);
        if (response == null || !Boolean.TRUE.equals(response.getValid())
                || response.getOurTeam() == null || response.getOpponentTeam() == null) {
            throw new IllegalStateException("Failed to validate match teams from team-service");
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public MatchResponse getMatch(Long id) {
        MatchInfo match = findMatch(id);
        return toResponse(match);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MatchResponse> getMatches(
            int page,
            int size,
            Long tournamentId,
            MatchStatus status,
            String keyword
    ) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "matchTime"));
        var matches = matchInfoRepository.findAll(matchSpecification(tournamentId, status, keyword), pageable);
        return new PageResponse<>(
                matches.getContent().stream().map(this::toResponse).toList(),
                matches.getNumber(),
                matches.getSize(),
                matches.getTotalElements(),
                matches.getTotalPages()
        );
    }

    private Specification<MatchInfo> matchSpecification(Long tournamentId, MatchStatus status, String keyword) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (tournamentId != null) {
                predicates.add(criteriaBuilder.equal(root.get("tournamentId"), tournamentId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("matchStatus"), status));
            }
            String normalizedKeyword = trimToNull(keyword);
            if (normalizedKeyword != null) {
                String pattern = "%" + normalizedKeyword.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("opponentTeamNameSnapshot")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("tournamentNameSnapshot")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("venue")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("roundStage")), pattern)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    @Override
    @Transactional
    public MatchResponse updateResult(Long id, UpdateMatchResultRequest request) {
        MatchInfo match = findMatch(id);

        Integer oldOurScore = match.getOurScore();
        Integer oldOpponentScore = match.getOpponentScore();

        match.setOurScore(request.getOurScore());
        match.setOpponentScore(request.getOpponentScore());
        match.setFinished(request.getFinished());
        if (Boolean.TRUE.equals(request.getFinished())) {
            match.setMatchStatus(MatchStatus.FINISHED);
        }

        MatchResultHistory history = new MatchResultHistory();
        history.setMatch(match);
        history.setOldOurScore(oldOurScore);
        history.setOldOpponentScore(oldOpponentScore);
        history.setNewOurScore(request.getOurScore());
        history.setNewOpponentScore(request.getOpponentScore());
        history.setRemark(request.getRemark());
        matchResultHistoryRepository.save(history);

        outboxEventService.saveEvent(
                AGGREGATE_TYPE_MATCH,
                match.getId(),
                "match.result.updated",
                MatchEventPayloadFactory.matchResultUpdated(match, oldOurScore, oldOpponentScore)
        );

        return toResponse(match);
    }

    @Override
    @Transactional
    public MatchResponse replaceAppearances(Long id, ReplaceAppearanceRequest request) {
        MatchInfo match = findMatch(id);
        validateAppearanceRequest(request);

        matchAppearanceRepository.deleteByMatchId(match.getId());
        matchAppearanceRepository.flush();

        List<MatchAppearance> appearances = request.getPlayers().stream()
                .map(player -> toAppearance(match, player))
                .toList();
        matchAppearanceRepository.saveAll(appearances);

        outboxEventService.saveEvent(
                AGGREGATE_TYPE_MATCH,
                match.getId(),
                "match.appearance.updated",
                MatchEventPayloadFactory.matchAppearanceUpdated(match, appearances)
        );

        return toResponse(match);
    }

    private MatchInfo findMatch(Long id) {
        return matchInfoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Match not found"));
    }

    private void validateAppearanceRequest(ReplaceAppearanceRequest request) {
        Set<Long> playerIds = new HashSet<>();
        for (PlayerAppearanceRequest player : request.getPlayers()) {
            if (!playerIds.add(player.getPlayerId())) {
                throw new IllegalArgumentException("Duplicate playerId in appearance list: " + player.getPlayerId());
            }
            if (player.getOnMinute() != null && player.getOffMinute() != null
                    && player.getOffMinute() < player.getOnMinute()) {
                throw new IllegalArgumentException("offMinute must be greater than or equal to onMinute");
            }
        }

        ValidatePlayersResponse response;
        try {
            response = teamServiceClient.validatePlayers(playerIds);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to validate players from team-service", ex);
        }

        if (response == null) {
            throw new IllegalStateException("Failed to validate players from team-service");
        }
        if (response.getInvalidPlayerIds() != null && !response.getInvalidPlayerIds().isEmpty()) {
            throw new IllegalArgumentException("Invalid playerIds: " + response.getInvalidPlayerIds());
        }
    }

    private MatchAppearance toAppearance(MatchInfo match, PlayerAppearanceRequest player) {
        MatchAppearance appearance = new MatchAppearance();
        appearance.setMatch(match);
        appearance.setPlayerId(player.getPlayerId());
        appearance.setPlayerNameSnapshot(player.getPlayerNameSnapshot());
        appearance.setJerseyNumberSnapshot(player.getJerseyNumberSnapshot());
        appearance.setPositionSnapshot(player.getPositionSnapshot());
        appearance.setAppeared(true);
        appearance.setStarter(player.getStarter());
        appearance.setOnMinute(player.getOnMinute());
        appearance.setOffMinute(player.getOffMinute());
        appearance.setRemark(player.getRemark());
        return appearance;
    }

    private MatchResponse toResponse(MatchInfo match) {
        List<MatchAppearanceResponse> appearances = matchAppearanceRepository
                .findByMatchIdOrderByStarterDescPlayerIdAsc(match.getId())
                .stream()
                .map(this::toAppearanceResponse)
                .toList();

        return new MatchResponse(
                match.getId(),
                match.getTournamentId(),
                match.getTournamentNameSnapshot(),
                match.getSeasonSnapshot(),
                match.getOurTeamId(),
                match.getOurTeamNameSnapshot(),
                match.getOpponentTeamId(),
                match.getOpponentTeamNameSnapshot(),
                match.getMatchTime(),
                match.getHomeAway(),
                match.getVenue(),
                match.getRoundStage(),
                match.getMatchStatus(),
                match.getOurScore(),
                match.getOpponentScore(),
                match.getFinished(),
                match.getCreatedAt(),
                match.getUpdatedAt(),
                match.getVersion(),
                appearances
        );
    }

    private MatchAppearanceResponse toAppearanceResponse(MatchAppearance appearance) {
        return new MatchAppearanceResponse(
                appearance.getId(),
                appearance.getPlayerId(),
                appearance.getPlayerNameSnapshot(),
                appearance.getJerseyNumberSnapshot(),
                appearance.getPositionSnapshot(),
                appearance.getAppeared(),
                appearance.getStarter(),
                appearance.getOnMinute(),
                appearance.getOffMinute(),
                appearance.getRemark()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
