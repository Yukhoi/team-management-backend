package com.yukai.team.matchservice.service;

import com.yukai.team.matchservice.client.TeamServiceClient;
import com.yukai.team.matchservice.client.dto.ValidatePlayersResponse;
import com.yukai.team.matchservice.dto.AssistResponse;
import com.yukai.team.matchservice.dto.CreateGoalRequest;
import com.yukai.team.matchservice.dto.GoalResponse;
import com.yukai.team.matchservice.dto.UpdateGoalRequest;
import com.yukai.team.matchservice.dto.UpsertAssistRequest;
import com.yukai.team.matchservice.entity.AssistRecord;
import com.yukai.team.matchservice.entity.GoalRecord;
import com.yukai.team.matchservice.entity.GoalType;
import com.yukai.team.matchservice.entity.MatchAppearance;
import com.yukai.team.matchservice.entity.MatchInfo;
import com.yukai.team.matchservice.entity.MatchStatus;
import com.yukai.team.matchservice.outbox.MatchEventPayloadFactory;
import com.yukai.team.matchservice.repository.AssistRecordRepository;
import com.yukai.team.matchservice.repository.GoalRecordRepository;
import com.yukai.team.matchservice.repository.MatchAppearanceRepository;
import com.yukai.team.matchservice.repository.MatchInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class GoalServiceImpl implements GoalService {

    private static final String AGGREGATE_TYPE_MATCH = "match";

    private final MatchInfoRepository matchInfoRepository;
    private final GoalRecordRepository goalRecordRepository;
    private final AssistRecordRepository assistRecordRepository;
    private final MatchAppearanceRepository matchAppearanceRepository;
    private final TeamServiceClient teamServiceClient;
    private final OutboxEventService outboxEventService;

    public GoalServiceImpl(
            MatchInfoRepository matchInfoRepository,
            GoalRecordRepository goalRecordRepository,
            AssistRecordRepository assistRecordRepository,
            MatchAppearanceRepository matchAppearanceRepository,
            TeamServiceClient teamServiceClient,
            OutboxEventService outboxEventService
    ) {
        this.matchInfoRepository = matchInfoRepository;
        this.goalRecordRepository = goalRecordRepository;
        this.assistRecordRepository = assistRecordRepository;
        this.matchAppearanceRepository = matchAppearanceRepository;
        this.teamServiceClient = teamServiceClient;
        this.outboxEventService = outboxEventService;
    }

    @Override
    @Transactional
    public GoalResponse createGoal(Long matchId, CreateGoalRequest request) {
        MatchInfo match = findMatch(matchId);
        validateMatchNotCancelled(match);

        GoalRecord goal = new GoalRecord();
        goal.setMatch(match);
        goal.setGoalMinute(request.getGoalMinute());
        goal.setGoalType(request.getGoalType());
        goal.setRemark(request.getRemark());
        applyGoalPlayer(match.getId(), goal, request.getPlayerId(), request.getGoalType());

        GoalRecord savedGoal = goalRecordRepository.save(goal);
        outboxEventService.saveEvent(
                AGGREGATE_TYPE_MATCH,
                savedGoal.getMatch().getId(),
                "match.goal.created",
                MatchEventPayloadFactory.goalCreated(savedGoal)
        );
        return toGoalResponse(savedGoal);
    }

    @Override
    @Transactional
    public GoalResponse updateGoal(Long goalId, UpdateGoalRequest request) {
        GoalRecord goal = findGoal(goalId);
        MatchInfo match = goal.getMatch();
        validateMatchNotCancelled(match);
        Map<String, Object> oldGoalSnapshot = MatchEventPayloadFactory.goalSnapshot(goal);

        goal.setGoalMinute(request.getGoalMinute());
        goal.setGoalType(request.getGoalType());
        goal.setRemark(request.getRemark());
        applyGoalPlayer(match.getId(), goal, request.getPlayerId(), request.getGoalType());
        enforceAssistCompatibility(goal);

        GoalRecord savedGoal = goalRecordRepository.save(goal);
        outboxEventService.saveEvent(
                AGGREGATE_TYPE_MATCH,
                savedGoal.getMatch().getId(),
                "match.goal.updated",
                MatchEventPayloadFactory.goalUpdated(savedGoal, oldGoalSnapshot)
        );
        return toGoalResponse(savedGoal);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getGoals(Long matchId) {
        findMatch(matchId);
        return goalRecordRepository.findByMatchIdOrderByGoalMinuteAscIdAsc(matchId)
                .stream()
                .map(this::toGoalResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteGoal(Long goalId) {
        GoalRecord goal = findGoal(goalId);
        Long matchId = goal.getMatch().getId();
        AssistRecord linkedAssist = assistRecordRepository.findByGoalRecordId(goal.getId()).orElse(null);
        Map<String, Object> goalDeletedPayload = MatchEventPayloadFactory.goalDeleted(goal, linkedAssist);

        outboxEventService.saveEvent(AGGREGATE_TYPE_MATCH, matchId, "match.goal.deleted", goalDeletedPayload);
        goalRecordRepository.delete(goal);
    }

    @Override
    @Transactional
    public AssistResponse upsertAssist(Long goalId, UpsertAssistRequest request) {
        GoalRecord goal = findGoal(goalId);
        MatchInfo match = goal.getMatch();
        validateMatchNotCancelled(match);
        validateGoalAllowsAssist(goal);

        MatchAppearance appearance = resolveAppearance(
                match.getId(),
                request.getPlayerId(),
                "Assist player did not appear in this match: "
        );
        if (Objects.equals(request.getPlayerId(), goal.getPlayerId())) {
            throw new IllegalArgumentException("assist player cannot be same as goal player");
        }

        AssistRecord assist = assistRecordRepository.findByGoalRecordId(goal.getId())
                .orElse(null);
        Map<String, Object> oldAssistSnapshot = assist == null
                ? null
                : MatchEventPayloadFactory.assistSnapshot(assist);
        if (assist == null) {
            assist = new AssistRecord();
            assist.setGoalRecord(goal);
        }
        assist.setPlayerId(appearance.getPlayerId());
        assist.setPlayerNameSnapshot(appearance.getPlayerNameSnapshot());
        assist.setJerseyNumberSnapshot(appearance.getJerseyNumberSnapshot());
        assist.setAssistMinute(request.getAssistMinute());
        assist.setRemark(request.getRemark());

        AssistRecord savedAssist = assistRecordRepository.save(assist);
        outboxEventService.saveEvent(
                AGGREGATE_TYPE_MATCH,
                savedAssist.getGoalRecord().getMatch().getId(),
                "match.assist.upserted",
                MatchEventPayloadFactory.assistUpserted(savedAssist, oldAssistSnapshot)
        );
        return toAssistResponse(savedAssist);
    }

    @Override
    @Transactional
    public void deleteAssist(Long goalId) {
        GoalRecord goal = findGoal(goalId);
        validateMatchNotCancelled(goal.getMatch());

        assistRecordRepository.findByGoalRecordId(goal.getId()).ifPresent(assist -> {
            Long matchId = goal.getMatch().getId();
            Map<String, Object> assistDeletedPayload = MatchEventPayloadFactory.assistDeleted(assist);
            assistRecordRepository.delete(assist);
            outboxEventService.saveEvent(
                    AGGREGATE_TYPE_MATCH,
                    matchId,
                    "match.assist.deleted",
                    assistDeletedPayload
            );
        });
    }

    private void applyGoalPlayer(Long matchId, GoalRecord goal, Long playerId, GoalType goalType) {
        if (goalType == GoalType.OWN_GOAL) {
            if (playerId != null) {
                throw new IllegalArgumentException("OWN_GOAL must not have playerId");
            }
            goal.setPlayerId(null);
            goal.setPlayerNameSnapshot(null);
            goal.setJerseyNumberSnapshot(null);
            return;
        }

        if (playerId == null) {
            throw new IllegalArgumentException("playerId is required for " + goalType);
        }

        MatchAppearance appearance = resolveAppearance(matchId, playerId, "Player did not appear in this match: ");
        goal.setPlayerId(appearance.getPlayerId());
        goal.setPlayerNameSnapshot(appearance.getPlayerNameSnapshot());
        goal.setJerseyNumberSnapshot(appearance.getJerseyNumberSnapshot());
    }

    private MatchAppearance resolveAppearance(Long matchId, Long playerId, String missingMessagePrefix) {
        validatePlayersFromTeamService(Set.of(playerId));
        return matchAppearanceRepository.findByMatchIdAndPlayerId(matchId, playerId)
                .orElseThrow(() -> new IllegalArgumentException(missingMessagePrefix + playerId));
    }

    private void validatePlayersFromTeamService(Set<Long> playerIds) {
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

    private void enforceAssistCompatibility(GoalRecord goal) {
        assistRecordRepository.findByGoalRecordId(goal.getId()).ifPresent(assist -> {
            if (goal.getGoalType() == GoalType.OWN_GOAL || goal.getGoalType() == GoalType.PENALTY) {
                Map<String, Object> assistDeletedPayload = MatchEventPayloadFactory.assistDeleted(assist);
                assistRecordRepository.delete(assist);
                outboxEventService.saveEvent(
                        AGGREGATE_TYPE_MATCH,
                        goal.getMatch().getId(),
                        "match.assist.deleted",
                        assistDeletedPayload
                );
                return;
            }
            if (Objects.equals(assist.getPlayerId(), goal.getPlayerId())) {
                throw new IllegalArgumentException("assist player cannot be same as goal player");
            }
        });
    }

    private void validateGoalAllowsAssist(GoalRecord goal) {
        if (goal.getGoalType() == GoalType.OWN_GOAL) {
            throw new IllegalArgumentException("OWN_GOAL does not allow assist");
        }
        if (goal.getGoalType() == GoalType.PENALTY) {
            throw new IllegalArgumentException("PENALTY does not allow assist");
        }
    }

    private void validateMatchNotCancelled(MatchInfo match) {
        if (match.getMatchStatus() == MatchStatus.CANCELLED) {
            throw new IllegalArgumentException("Cancelled match does not allow goal or assist changes");
        }
    }

    private MatchInfo findMatch(Long matchId) {
        return matchInfoRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found"));
    }

    private GoalRecord findGoal(Long goalId) {
        return goalRecordRepository.findById(goalId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found"));
    }

    private GoalResponse toGoalResponse(GoalRecord goal) {
        AssistResponse assist = assistRecordRepository.findByGoalRecordId(goal.getId())
                .map(this::toAssistResponse)
                .orElse(null);
        return new GoalResponse(
                goal.getId(),
                goal.getMatch().getId(),
                goal.getPlayerId(),
                goal.getPlayerNameSnapshot(),
                goal.getJerseyNumberSnapshot(),
                goal.getGoalMinute(),
                goal.getGoalType(),
                goal.getRemark(),
                assist,
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }

    private AssistResponse toAssistResponse(AssistRecord assist) {
        return new AssistResponse(
                assist.getId(),
                assist.getGoalRecord().getId(),
                assist.getPlayerId(),
                assist.getPlayerNameSnapshot(),
                assist.getJerseyNumberSnapshot(),
                assist.getAssistMinute(),
                assist.getRemark()
        );
    }

}
