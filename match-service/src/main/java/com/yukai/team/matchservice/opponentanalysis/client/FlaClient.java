package com.yukai.team.matchservice.opponentanalysis.client;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;

import java.util.List;

public interface FlaClient {

    List<FlaStandingEntryResponse> getStandings(Long championnatId, Long saisonId);
}
