package com.yukai.team.matchservice.opponentanalysis.dto;

import java.util.List;

/** Identity observed on the source page. Unknown season identity must remain null. */
public record FlaTeamMatchesPageResponse(Long flaTeamId, Long championnatId, Long saisonId,
                                         List<FlaTeamMatchResponse> matches) {
    public FlaTeamMatchesPageResponse {
        matches = List.copyOf(matches);
    }
}
