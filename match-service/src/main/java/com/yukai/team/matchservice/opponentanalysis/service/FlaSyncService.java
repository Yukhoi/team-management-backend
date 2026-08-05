package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaSyncResponse;

public interface FlaSyncService {

    FlaSyncResponse syncStandings(Long championnatId, Long saisonId, String championnatName);
}
