package com.yukai.team.matchservice.opponentanalysis.dto;

import com.yukai.team.matchservice.opponentanalysis.dto.ai.GeneratedOpponentReport;
import com.yukai.team.matchservice.opponentanalysis.entity.OpponentAnalysisStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Persisted opponent analysis report response")
public record OpponentAnalysisReportResponse(
        Long id,
        Long matchId,
        Long opponentTeamId,
        String opponentTeamName,
        OpponentAnalysisStatus status,
        String provider,
        String model,
        String promptVersion,
        String language,
        Boolean reused,
        Long snapshotId,
        OffsetDateTime sourceFetchedAt,
        OffsetDateTime generatedAt,
        OffsetDateTime createdAt,
        MatchOpponentMetricsResponse metrics,
        GeneratedOpponentReport report,
        Usage usage,
        String errorCode
) {
    public record Usage(
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens
    ) {
    }
}
