package com.yukai.team.matchservice.opponentanalysis.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** A match normalized to home/away order; unavailable external IDs remain null. */
public record FlaTeamMatchResponse(
        LocalDate matchDate,
        LocalTime kickoffTime,
        Long homeTeamId,
        String homeTeamName,
        Long awayTeamId,
        String awayTeamName,
        Integer homeScore,
        Integer awayScore,
        CompetitionType competitionType,
        String competitionName,
        String competitionRound,
        MatchStatus status
) {
    public enum MatchStatus { COMPLETED, SCHEDULED, UNKNOWN }

    public enum CompetitionType { LEAGUE, CUP, OTHER }

    public enum MatchResult { WIN, DRAW, LOSS }

    public boolean hasCompletedScore() {
        return status == MatchStatus.COMPLETED
                && homeScore != null && awayScore != null && homeScore >= 0 && awayScore >= 0;
    }

    /** Returns null for unfinished matches or a team whose identity is unavailable. */
    public MatchResult resultFor(Long teamId) {
        if (teamId == null || !hasCompletedScore()) {
            return null;
        }
        int difference;
        if (teamId.equals(homeTeamId)) {
            difference = Integer.compare(homeScore, awayScore);
        } else if (teamId.equals(awayTeamId)) {
            difference = Integer.compare(awayScore, homeScore);
        } else {
            return null;
        }
        return difference > 0 ? MatchResult.WIN : difference < 0 ? MatchResult.LOSS : MatchResult.DRAW;
    }
}
