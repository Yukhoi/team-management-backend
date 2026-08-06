package com.yukai.team.matchservice.opponentanalysis.dto.ai;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(name = "OpponentReportGenerationResult", description = "Development OpenRouter test generation result")
public record OpponentReportGenerationResult(
        @Schema(description = "AI provider", example = "openrouter")
        String provider,
        @Schema(description = "AI model", example = "inclusionai/ling-3.0-flash")
        String model,
        @Schema(description = "Prompt version", example = "v1")
        String promptVersion,
        @Schema(description = "Generated structured AI report")
        GeneratedOpponentReport report,
        @Schema(description = "OpenRouter token usage")
        Usage usage,
        @Schema(description = "Generation time", example = "2026-08-06T10:15:30Z")
        OffsetDateTime generatedAt
) {
    @Schema(name = "OpponentReportGenerationUsage", description = "Development test AI provider token usage")
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
