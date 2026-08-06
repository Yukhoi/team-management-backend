package com.yukai.team.matchservice.opponentanalysis.dto;

import com.yukai.team.matchservice.opponentanalysis.dto.ai.GeneratedOpponentReport;
import com.yukai.team.matchservice.opponentanalysis.entity.OpponentAnalysisStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(name = "OpponentAnalysisReportResponse", description = "Persisted opponent analysis report response")
public record OpponentAnalysisReportResponse(
        @Schema(description = "Report ID", example = "10")
        Long id,
        @Schema(description = "Match ID", example = "42")
        Long matchId,
        @Schema(description = "Opponent internal team ID", example = "13")
        Long opponentTeamId,
        @Schema(description = "Opponent team name", example = "ASTERIA")
        String opponentTeamName,
        @Schema(description = "Report generation status", implementation = OpponentAnalysisStatus.class, example = "COMPLETED")
        OpponentAnalysisStatus status,
        @Schema(description = "AI provider", example = "openrouter")
        String provider,
        @Schema(description = "AI model", example = "inclusionai/ling-3.0-flash")
        String model,
        @Schema(description = "Prompt version", example = "v1")
        String promptVersion,
        @Schema(description = "Report language", example = "zh-CN")
        String language,
        @Schema(description = "Whether this response reused an existing completed report", example = "false")
        Boolean reused,
        @Schema(description = "FLA standings snapshot ID used for the report", example = "23")
        Long snapshotId,
        @Schema(description = "Time when the source FLA snapshot was fetched", example = "2026-08-05T12:00:00Z")
        OffsetDateTime sourceFetchedAt,
        @Schema(description = "Time when the AI report was generated", example = "2026-08-06T10:15:30Z")
        OffsetDateTime generatedAt,
        @Schema(description = "Time when the report record was created", example = "2026-08-06T10:15:20Z")
        OffsetDateTime createdAt,
        @Schema(description = "Deterministic metrics used as report input")
        MatchOpponentMetricsResponse metrics,
        @Schema(description = "Generated structured AI report")
        GeneratedOpponentReport report,
        @Schema(description = "OpenRouter token usage")
        Usage usage,
        @Schema(description = "Error code when status is FAILED", example = "OPENROUTER_UPSTREAM_ERROR")
        String errorCode
) {
    @Schema(name = "Usage", description = "AI provider token usage")
    public record Usage(
            @Schema(description = "Prompt token count", example = "1200")
            Integer promptTokens,
            @Schema(description = "Completion token count", example = "700")
            Integer completionTokens,
            @Schema(description = "Total token count", example = "1900")
            Integer totalTokens
    ) {
    }
}
