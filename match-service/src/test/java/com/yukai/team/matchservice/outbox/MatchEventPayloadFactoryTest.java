package com.yukai.team.matchservice.outbox;

import com.yukai.team.matchservice.entity.AssistRecord;
import com.yukai.team.matchservice.entity.GoalRecord;
import com.yukai.team.matchservice.entity.GoalType;
import com.yukai.team.matchservice.entity.HomeAway;
import com.yukai.team.matchservice.entity.MatchAppearance;
import com.yukai.team.matchservice.entity.MatchInfo;
import com.yukai.team.matchservice.entity.MatchStatus;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MatchEventPayloadFactoryTest {

    @Test
    void should_build_complete_match_created_payload() {
        MatchInfo match = buildMatch();

        Map<String, Object> payload = MatchEventPayloadFactory.matchCreated(match);

        assertEquals(1L, payload.get("matchId"));
        assertEquals(10L, payload.get("tournamentId"));
        assertEquals("Spring Cup", payload.get("tournamentNameSnapshot"));
        assertEquals("2026", payload.get("seasonSnapshot"));
        assertEquals(100L, payload.get("ourTeamId"));
        assertEquals("Our Team", payload.get("ourTeamNameSnapshot"));
        assertEquals(200L, payload.get("opponentTeamId"));
        assertEquals("Opponent A", payload.get("opponentTeamNameSnapshot"));
        assertEquals(HomeAway.HOME, payload.get("homeAway"));
        assertEquals(0, payload.get("ourScore"));
        assertEquals(0, payload.get("opponentScore"));
        assertEquals(MatchStatus.SCHEDULED, payload.get("matchStatus"));
        assertEquals(false, payload.get("finished"));
    }

    @Test
    void should_build_match_result_updated_payload_with_old_and_new_scores() {
        MatchInfo match = buildMatch();
        match.setOurScore(2);
        match.setOpponentScore(1);
        match.setMatchStatus(MatchStatus.FINISHED);
        match.setFinished(true);

        Map<String, Object> payload = MatchEventPayloadFactory.matchResultUpdated(match, 0, 0);

        assertEquals(0, payload.get("oldOurScore"));
        assertEquals(0, payload.get("oldOpponentScore"));
        assertEquals(2, payload.get("newOurScore"));
        assertEquals(1, payload.get("newOpponentScore"));
        assertEquals(MatchStatus.FINISHED, payload.get("matchStatus"));
        assertEquals(true, payload.get("finished"));
    }

    @Test
    void should_build_match_appearance_updated_payload_with_full_appearance_snapshot() {
        MatchInfo match = buildMatch();
        MatchAppearance starter = buildAppearance(match, 11L, "Alice", 10, "FW", true, 0, 80);
        MatchAppearance substitute = buildAppearance(match, 12L, "Bob", 8, "MF", false, 60, null);

        Map<String, Object> payload = MatchEventPayloadFactory.matchAppearanceUpdated(
                match,
                List.of(starter, substitute)
        );

        List<?> appearances = (List<?>) payload.get("appearances");
        assertEquals(2, appearances.size());
        Map<?, ?> first = (Map<?, ?>) appearances.get(0);
        Map<?, ?> second = (Map<?, ?>) appearances.get(1);
        assertEquals(11L, first.get("playerId"));
        assertEquals("Alice", first.get("playerNameSnapshot"));
        assertEquals(10, first.get("jerseyNumberSnapshot"));
        assertEquals("FW", first.get("positionSnapshot"));
        assertEquals(true, first.get("starter"));
        assertEquals(0, first.get("onMinute"));
        assertEquals(80, first.get("offMinute"));
        assertEquals(12L, second.get("playerId"));
        assertNull(second.get("offMinute"));
    }

    @Test
    void should_build_goal_created_payload_with_player_and_tournament_snapshot() {
        GoalRecord goal = buildGoal(buildMatch(), 21L, "Charles", 9, GoalType.NORMAL);
        goal.setGoalMinute(36);

        Map<String, Object> payload = MatchEventPayloadFactory.goalCreated(goal);

        assertEquals(31L, payload.get("goalId"));
        assertEquals(1L, payload.get("matchId"));
        assertEquals(10L, payload.get("tournamentId"));
        assertEquals("Spring Cup", payload.get("tournamentNameSnapshot"));
        assertEquals("2026", payload.get("seasonSnapshot"));
        assertEquals(21L, payload.get("playerId"));
        assertEquals("Charles", payload.get("playerNameSnapshot"));
        assertEquals(9, payload.get("jerseyNumberSnapshot"));
        assertEquals(36, payload.get("goalMinute"));
        assertEquals(GoalType.NORMAL, payload.get("goalType"));
    }

    @Test
    void should_build_goal_created_payload_with_null_player_for_own_goal() {
        GoalRecord goal = buildGoal(buildMatch(), null, null, null, GoalType.OWN_GOAL);

        Map<String, Object> payload = MatchEventPayloadFactory.goalCreated(goal);

        assertNull(payload.get("playerId"));
        assertNull(payload.get("playerNameSnapshot"));
        assertNull(payload.get("jerseyNumberSnapshot"));
        assertEquals(GoalType.OWN_GOAL, payload.get("goalType"));
    }

    @Test
    void should_build_goal_updated_payload_with_old_and_new_goal_snapshots() {
        GoalRecord goal = buildGoal(buildMatch(), 21L, "Charles", 9, GoalType.NORMAL);
        Map<String, Object> oldGoal = MatchEventPayloadFactory.goalSnapshot(goal);
        goal.setPlayerId(22L);
        goal.setPlayerNameSnapshot("David");
        goal.setJerseyNumberSnapshot(7);
        goal.setGoalType(GoalType.PENALTY);

        Map<String, Object> payload = MatchEventPayloadFactory.goalUpdated(goal, oldGoal);

        Map<?, ?> oldSnapshot = (Map<?, ?>) payload.get("oldGoal");
        Map<?, ?> newSnapshot = (Map<?, ?>) payload.get("newGoal");
        assertEquals(21L, oldSnapshot.get("playerId"));
        assertEquals("Charles", oldSnapshot.get("playerNameSnapshot"));
        assertEquals(GoalType.NORMAL, oldSnapshot.get("goalType"));
        assertEquals(22L, newSnapshot.get("playerId"));
        assertEquals("David", newSnapshot.get("playerNameSnapshot"));
        assertEquals(GoalType.PENALTY, newSnapshot.get("goalType"));
    }

    @Test
    void should_build_goal_deleted_payload_from_deleted_goal_snapshot() {
        GoalRecord goal = buildGoal(buildMatch(), 21L, "Charles", 9, GoalType.NORMAL);

        Map<String, Object> payload = MatchEventPayloadFactory.goalDeleted(goal, null);

        assertEquals(31L, payload.get("goalId"));
        assertEquals(1L, payload.get("matchId"));
        assertEquals(21L, payload.get("playerId"));
        assertEquals("Charles", payload.get("playerNameSnapshot"));
        assertEquals(9, payload.get("jerseyNumberSnapshot"));
        assertEquals(GoalType.NORMAL, payload.get("goalType"));
        assertNull(payload.get("linkedAssist"));
    }

    @Test
    void should_build_goal_deleted_payload_with_linked_assist_snapshot() {
        GoalRecord goal = buildGoal(buildMatch(), 21L, "Charles", 9, GoalType.NORMAL);
        AssistRecord assist = buildAssist(goal, 41L, "Evan", 8, 36);

        Map<String, Object> payload = MatchEventPayloadFactory.goalDeleted(goal, assist);

        Map<?, ?> linkedAssist = (Map<?, ?>) payload.get("linkedAssist");
        assertEquals(31L, payload.get("goalId"));
        assertEquals(51L, linkedAssist.get("assistId"));
        assertEquals(41L, linkedAssist.get("playerId"));
        assertEquals("Evan", linkedAssist.get("playerNameSnapshot"));
        assertEquals(8, linkedAssist.get("jerseyNumberSnapshot"));
        assertEquals(36, linkedAssist.get("assistMinute"));
    }

    @Test
    void should_build_assist_upserted_payload_with_old_and_new_assist_snapshots() {
        AssistRecord assist = buildAssist(buildGoal(buildMatch(), 21L, "Charles", 9, GoalType.NORMAL),
                41L, "Evan", 8, 36);
        Map<String, Object> oldAssist = MatchEventPayloadFactory.assistSnapshot(assist);
        assist.setPlayerId(42L);
        assist.setPlayerNameSnapshot("Frank");
        assist.setJerseyNumberSnapshot(11);

        Map<String, Object> payload = MatchEventPayloadFactory.assistUpserted(assist, oldAssist);

        Map<?, ?> oldSnapshot = (Map<?, ?>) payload.get("oldAssist");
        Map<?, ?> newSnapshot = (Map<?, ?>) payload.get("newAssist");
        assertEquals(51L, payload.get("assistId"));
        assertEquals(31L, payload.get("goalId"));
        assertEquals(41L, oldSnapshot.get("playerId"));
        assertEquals("Evan", oldSnapshot.get("playerNameSnapshot"));
        assertEquals(42L, newSnapshot.get("playerId"));
        assertEquals("Frank", newSnapshot.get("playerNameSnapshot"));
        assertEquals(36, newSnapshot.get("assistMinute"));
    }

    @Test
    void should_build_assist_deleted_payload_from_deleted_assist_snapshot() {
        AssistRecord assist = buildAssist(buildGoal(buildMatch(), 21L, "Charles", 9, GoalType.NORMAL),
                41L, "Evan", 8, 36);

        Map<String, Object> payload = MatchEventPayloadFactory.assistDeleted(assist);

        assertEquals(51L, payload.get("assistId"));
        assertEquals(31L, payload.get("goalId"));
        assertEquals(1L, payload.get("matchId"));
        assertEquals(41L, payload.get("playerId"));
        assertEquals("Evan", payload.get("playerNameSnapshot"));
        assertEquals(8, payload.get("jerseyNumberSnapshot"));
        assertEquals(36, payload.get("assistMinute"));
    }

    private MatchInfo buildMatch() {
        MatchInfo match = new MatchInfo();
        match.setId(1L);
        match.setTournamentId(10L);
        match.setTournamentNameSnapshot("Spring Cup");
        match.setSeasonSnapshot("2026");
        match.setOurTeamId(100L);
        match.setOurTeamNameSnapshot("Our Team");
        match.setOpponentTeamId(200L);
        match.setOpponentTeamNameSnapshot("Opponent A");
        match.setMatchTime(OffsetDateTime.parse("2026-05-20T20:00:00+02:00"));
        match.setHomeAway(HomeAway.HOME);
        match.setOurScore(0);
        match.setOpponentScore(0);
        match.setMatchStatus(MatchStatus.SCHEDULED);
        match.setFinished(false);
        return match;
    }

    private MatchAppearance buildAppearance(
            MatchInfo match,
            Long playerId,
            String playerName,
            Integer jerseyNumber,
            String position,
            Boolean starter,
            Integer onMinute,
            Integer offMinute
    ) {
        MatchAppearance appearance = new MatchAppearance();
        appearance.setMatch(match);
        appearance.setPlayerId(playerId);
        appearance.setPlayerNameSnapshot(playerName);
        appearance.setJerseyNumberSnapshot(jerseyNumber);
        appearance.setPositionSnapshot(position);
        appearance.setAppeared(true);
        appearance.setStarter(starter);
        appearance.setOnMinute(onMinute);
        appearance.setOffMinute(offMinute);
        return appearance;
    }

    private GoalRecord buildGoal(
            MatchInfo match,
            Long playerId,
            String playerName,
            Integer jerseyNumber,
            GoalType goalType
    ) {
        GoalRecord goal = new GoalRecord();
        goal.setId(31L);
        goal.setMatch(match);
        goal.setPlayerId(playerId);
        goal.setPlayerNameSnapshot(playerName);
        goal.setJerseyNumberSnapshot(jerseyNumber);
        goal.setGoalMinute(36);
        goal.setGoalType(goalType);
        return goal;
    }

    private AssistRecord buildAssist(
            GoalRecord goal,
            Long playerId,
            String playerName,
            Integer jerseyNumber,
            Integer assistMinute
    ) {
        AssistRecord assist = new AssistRecord();
        assist.setId(51L);
        assist.setGoalRecord(goal);
        assist.setPlayerId(playerId);
        assist.setPlayerNameSnapshot(playerName);
        assist.setJerseyNumberSnapshot(jerseyNumber);
        assist.setAssistMinute(assistMinute);
        return assist;
    }
}
