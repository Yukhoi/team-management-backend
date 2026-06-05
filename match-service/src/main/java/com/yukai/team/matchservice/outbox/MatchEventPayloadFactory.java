package com.yukai.team.matchservice.outbox;

import com.yukai.team.matchservice.entity.AssistRecord;
import com.yukai.team.matchservice.entity.GoalRecord;
import com.yukai.team.matchservice.entity.MatchAppearance;
import com.yukai.team.matchservice.entity.MatchInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MatchEventPayloadFactory {

    private MatchEventPayloadFactory() {
    }

    public static Map<String, Object> matchCreated(MatchInfo match) {
        Map<String, Object> payload = baseMatchSnapshot(match);
        payload.put("ourScore", match.getOurScore());
        payload.put("opponentScore", match.getOpponentScore());
        payload.put("matchStatus", match.getMatchStatus());
        payload.put("finished", match.getFinished());
        return payload;
    }

    public static Map<String, Object> matchResultUpdated(
            MatchInfo match,
            Integer oldOurScore,
            Integer oldOpponentScore
    ) {
        Map<String, Object> payload = baseMatchSnapshot(match);
        payload.put("oldOurScore", oldOurScore);
        payload.put("oldOpponentScore", oldOpponentScore);
        payload.put("newOurScore", match.getOurScore());
        payload.put("newOpponentScore", match.getOpponentScore());
        payload.put("matchStatus", match.getMatchStatus());
        payload.put("finished", match.getFinished());
        return payload;
    }

    public static Map<String, Object> matchAppearanceUpdated(
            MatchInfo match,
            List<MatchAppearance> appearances
    ) {
        Map<String, Object> payload = tournamentSnapshot(match);
        payload.put("matchId", match.getId());
        payload.put("appearances", appearances.stream()
                .map(MatchEventPayloadFactory::appearanceSnapshot)
                .toList());
        return payload;
    }

    public static Map<String, Object> goalCreated(GoalRecord goal) {
        Map<String, Object> payload = goalEventBase(goal);
        payload.putAll(goalSnapshot(goal));
        payload.put("goalMinute", goal.getGoalMinute());
        return payload;
    }

    public static Map<String, Object> goalUpdated(
            GoalRecord goal,
            Map<String, Object> oldGoalSnapshot
    ) {
        Map<String, Object> payload = goalEventBase(goal);
        payload.put("oldGoal", oldGoalSnapshot);
        payload.put("newGoal", goalSnapshot(goal));
        return payload;
    }

    public static Map<String, Object> goalDeleted(GoalRecord goal, AssistRecord linkedAssist) {
        Map<String, Object> payload = goalEventBase(goal);
        payload.putAll(goalSnapshot(goal));
        payload.put("linkedAssist", linkedAssist == null ? null : linkedAssistSnapshot(linkedAssist));
        return payload;
    }

    public static Map<String, Object> assistUpserted(
            AssistRecord assist,
            Map<String, Object> oldAssistSnapshot
    ) {
        GoalRecord goal = assist.getGoalRecord();
        Map<String, Object> payload = goalEventBase(goal);
        payload.put("assistId", assist.getId());
        payload.put("oldAssist", oldAssistSnapshot);
        payload.put("newAssist", assistSnapshot(assist));
        return payload;
    }

    public static Map<String, Object> assistDeleted(AssistRecord assist) {
        GoalRecord goal = assist.getGoalRecord();
        Map<String, Object> payload = goalEventBase(goal);
        payload.put("assistId", assist.getId());
        payload.putAll(assistSnapshot(assist));
        return payload;
    }

    public static Map<String, Object> goalSnapshot(GoalRecord goal) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("playerId", goal.getPlayerId());
        snapshot.put("playerNameSnapshot", goal.getPlayerNameSnapshot());
        snapshot.put("jerseyNumberSnapshot", goal.getJerseyNumberSnapshot());
        snapshot.put("goalType", goal.getGoalType());
        return snapshot;
    }

    public static Map<String, Object> assistSnapshot(AssistRecord assist) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("playerId", assist.getPlayerId());
        snapshot.put("playerNameSnapshot", assist.getPlayerNameSnapshot());
        snapshot.put("jerseyNumberSnapshot", assist.getJerseyNumberSnapshot());
        snapshot.put("assistMinute", assist.getAssistMinute());
        return snapshot;
    }

    private static Map<String, Object> linkedAssistSnapshot(AssistRecord assist) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("assistId", assist.getId());
        snapshot.putAll(assistSnapshot(assist));
        return snapshot;
    }

    private static Map<String, Object> goalEventBase(GoalRecord goal) {
        MatchInfo match = goal.getMatch();
        Map<String, Object> payload = tournamentSnapshot(match);
        payload.put("goalId", goal.getId());
        payload.put("matchId", match.getId());
        return payload;
    }

    private static Map<String, Object> baseMatchSnapshot(MatchInfo match) {
        Map<String, Object> payload = tournamentSnapshot(match);
        payload.put("matchId", match.getId());
        payload.put("ourTeamId", match.getOurTeamId());
        payload.put("ourTeamNameSnapshot", match.getOurTeamNameSnapshot());
        payload.put("opponentTeamId", match.getOpponentTeamId());
        payload.put("opponentTeamNameSnapshot", match.getOpponentTeamNameSnapshot());
        payload.put("matchTime", match.getMatchTime());
        payload.put("homeAway", match.getHomeAway());
        return payload;
    }

    private static Map<String, Object> tournamentSnapshot(MatchInfo match) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tournamentId", match.getTournamentId());
        payload.put("tournamentNameSnapshot", match.getTournamentNameSnapshot());
        payload.put("seasonSnapshot", match.getSeasonSnapshot());
        return payload;
    }

    private static Map<String, Object> appearanceSnapshot(MatchAppearance appearance) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("playerId", appearance.getPlayerId());
        snapshot.put("playerNameSnapshot", appearance.getPlayerNameSnapshot());
        snapshot.put("jerseyNumberSnapshot", appearance.getJerseyNumberSnapshot());
        snapshot.put("positionSnapshot", appearance.getPositionSnapshot());
        snapshot.put("appeared", appearance.getAppeared());
        snapshot.put("starter", appearance.getStarter());
        snapshot.put("onMinute", appearance.getOnMinute());
        snapshot.put("offMinute", appearance.getOffMinute());
        return snapshot;
    }
}
