package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.dto.ai.GeneratedOpponentReport;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.OpponentAnalysisInput;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.OpponentReportGenerationResult;

public interface OpponentReportGenerator {

    GeneratedOpponentReport generate(OpponentAnalysisInput input);

    OpponentReportGenerationResult generateWithMetadata(OpponentAnalysisInput input);
}
