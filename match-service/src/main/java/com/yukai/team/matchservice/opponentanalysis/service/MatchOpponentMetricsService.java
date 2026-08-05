package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.dto.MatchOpponentMetricsResponse;

public interface MatchOpponentMetricsService {

    MatchOpponentMetricsResponse getMetrics(Long matchId);
}
