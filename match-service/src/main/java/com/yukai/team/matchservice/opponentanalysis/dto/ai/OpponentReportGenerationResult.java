package com.yukai.team.matchservice.opponentanalysis.dto.ai;

import java.time.OffsetDateTime;

public record OpponentReportGenerationResult(
        String provider,
        String model,
        String promptVersion,
        GeneratedOpponentReport report,
        Usage usage,
        OffsetDateTime generatedAt
) {
    public record Usage(
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens
    ) {
    }
}
