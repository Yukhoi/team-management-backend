package com.yukai.team.matchservice.service;

import com.yukai.team.matchservice.client.TeamServiceClient;
import com.yukai.team.matchservice.client.dto.ValidatePlayersResponse;
import com.yukai.team.matchservice.dto.UpsertAssistRequest;
import com.yukai.team.matchservice.entity.AssistRecord;
import com.yukai.team.matchservice.entity.GoalRecord;
import com.yukai.team.matchservice.entity.GoalType;
import com.yukai.team.matchservice.entity.MatchAppearance;
import com.yukai.team.matchservice.entity.MatchInfo;
import com.yukai.team.matchservice.entity.MatchStatus;
import com.yukai.team.matchservice.repository.AssistRecordRepository;
import com.yukai.team.matchservice.repository.GoalRecordRepository;
import com.yukai.team.matchservice.repository.MatchAppearanceRepository;
import com.yukai.team.matchservice.repository.MatchInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.EntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GoalServiceImplTest {

    @Mock
    private MatchInfoRepository matchInfoRepository;

    @Mock
    private GoalRecordRepository goalRecordRepository;

    @Mock
    private AssistRecordRepository assistRecordRepository;

    @Mock
    private MatchAppearanceRepository matchAppearanceRepository;

    @Mock
    private TeamServiceClient teamServiceClient;

    @Mock
    private OutboxEventService outboxEventService;

    private GoalServiceImpl goalService;
    private GoalRecord goal;
    private MatchAppearance appearance;

    @BeforeEach
    void setUp() {
        goalService = new GoalServiceImpl(
                matchInfoRepository,
                goalRecordRepository,
                assistRecordRepository,
                matchAppearanceRepository,
                teamServiceClient,
                outboxEventService
        );

        MatchInfo match = new MatchInfo();
        match.setId(1L);
        match.setTournamentId(10L);
        match.setTournamentNameSnapshot("Spring Cup");
        match.setSeasonSnapshot("2026");
        match.setMatchStatus(MatchStatus.SCHEDULED);

        goal = new GoalRecord();
        goal.setId(2L);
        goal.setMatch(match);
        goal.setPlayerId(11L);
        goal.setGoalType(GoalType.NORMAL);

        appearance = new MatchAppearance();
        appearance.setPlayerId(22L);
        appearance.setPlayerNameSnapshot("Assist Player");
        appearance.setJerseyNumberSnapshot(8);

        lenient().when(goalRecordRepository.findById(2L)).thenReturn(Optional.of(goal));
        lenient().when(teamServiceClient.validatePlayers(anySet())).thenReturn(new ValidatePlayersResponse());
        lenient().when(matchAppearanceRepository.findByMatchIdAndPlayerId(1L, 22L)).thenReturn(Optional.of(appearance));
        lenient().when(assistRecordRepository.save(any(AssistRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void newAssistPublishesStandardUpsertEvent() {
        when(assistRecordRepository.findByGoalRecordId(2L)).thenReturn(Optional.empty());

        goalService.upsertAssist(2L, request());

        Map<String, Object> payload = capturedPayload();
        assertThat(payload.get("oldAssist")).isNull();
        assertThat(payload.get("newAssist")).isInstanceOf(Map.class);
    }

    @Test
    void updatedAssistPublishesStandardUpsertEvent() {
        AssistRecord existingAssist = new AssistRecord();
        existingAssist.setId(3L);
        existingAssist.setGoalRecord(goal);
        existingAssist.setPlayerId(33L);
        existingAssist.setPlayerNameSnapshot("Old Assist Player");
        existingAssist.setJerseyNumberSnapshot(9);
        when(assistRecordRepository.findByGoalRecordId(2L)).thenReturn(Optional.of(existingAssist));

        goalService.upsertAssist(2L, request());

        Map<String, Object> payload = capturedPayload();
        assertThat(payload.get("oldAssist")).isInstanceOf(Map.class);
        assertThat(payload.get("newAssist")).isInstanceOf(Map.class);
    }

    @Test
    void getGoalsReturnsGoalWithAssist() {
        AssistRecord assist = new AssistRecord();
        assist.setId(3L);
        assist.setGoalRecord(goal);
        assist.setPlayerId(22L);
        assist.setPlayerNameSnapshot("Assist Player");
        when(matchInfoRepository.findById(1L)).thenReturn(Optional.of(goal.getMatch()));
        when(goalRecordRepository.findByMatchIdOrderByGoalMinuteAscIdAsc(1L)).thenReturn(List.of(goal));
        when(assistRecordRepository.findByGoalRecordId(2L)).thenReturn(Optional.of(assist));

        var response = goalService.getGoals(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getAssist()).isNotNull();
        assertThat(response.get(0).getAssist().getPlayerId()).isEqualTo(22L);
    }

    @Test
    void getGoalsReturnsEmptyListWhenMatchHasNoGoals() {
        when(matchInfoRepository.findById(1L)).thenReturn(Optional.of(goal.getMatch()));
        when(goalRecordRepository.findByMatchIdOrderByGoalMinuteAscIdAsc(1L)).thenReturn(List.of());

        assertThat(goalService.getGoals(1L)).isEmpty();
    }

    @Test
    void getGoalsThrowsNotFoundWhenMatchDoesNotExist() {
        when(matchInfoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> goalService.getGoals(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Match not found");
    }

    private UpsertAssistRequest request() {
        UpsertAssistRequest request = new UpsertAssistRequest();
        request.setPlayerId(22L);
        request.setAssistMinute(36);
        return request;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedPayload() {
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(outboxEventService).saveEvent(
                eq("match"),
                eq(1L),
                eq("match.assist.upserted"),
                payloadCaptor.capture()
        );
        return (Map<String, Object>) payloadCaptor.getValue();
    }
}
