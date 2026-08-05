package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.dto.OpponentAnalysisReportResponse;

import java.util.List;

public interface OpponentAnalysisApplicationService {

    OpponentAnalysisReportResponse generate(Long matchId, boolean forceRefresh);

    OpponentAnalysisReportResponse latest(Long matchId);

    List<OpponentAnalysisReportResponse> history(Long matchId);

    OpponentAnalysisReportResponse get(Long reportId);
}
