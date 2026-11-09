package com.yukai.team.matchservice.opponentanalysis.client;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchResponse;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchesPageResponse;

import java.util.List;

public interface FlaClient {

    List<FlaStandingEntryResponse> getStandings(Long championnatId, Long saisonId);

    /** All dated matches published in the team's current season calendar, including scheduled matches. */
    List<FlaTeamMatchResponse> getTeamMatches(Long flaTeamId);

    FlaTeamMatchesPageResponse getTeamMatchesPage(Long flaTeamId);

    /** Completed, scored matches up to today's date in Europe/Paris, newest first. */
    List<FlaTeamMatchResponse> getRecentCompletedMatches(Long flaTeamId, int limit);
}
