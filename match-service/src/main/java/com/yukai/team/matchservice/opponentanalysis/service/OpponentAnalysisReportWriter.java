package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.entity.OpponentAnalysisReport;

import java.time.OffsetDateTime;

public interface OpponentAnalysisReportWriter {

    OpponentAnalysisReport createPending(
            Long matchId,
            Long opponentTeamId,
            Long snapshotId,
            String provider,
            String model,
            String promptVersion,
            String language,
            String metricsJson,
            OffsetDateTime sourceFetchedAt
    );

    OpponentAnalysisReport complete(
            Long reportId,
            String reportJson,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            OffsetDateTime generatedAt
    );

    OpponentAnalysisReport fail(Long reportId, String errorCode, String errorMessage);
}
