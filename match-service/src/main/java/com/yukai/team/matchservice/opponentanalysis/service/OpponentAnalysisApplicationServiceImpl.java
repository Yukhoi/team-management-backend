package com.yukai.team.matchservice.opponentanalysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.matchservice.entity.MatchInfo;
import com.yukai.team.matchservice.entity.MatchStatus;
import com.yukai.team.matchservice.opponentanalysis.config.OpponentAiProperties;
import com.yukai.team.matchservice.opponentanalysis.dto.MatchOpponentMetricsResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.OpponentAnalysisReportResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.GeneratedOpponentReport;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.OpponentAnalysisInput;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.OpponentReportGenerationResult;
import com.yukai.team.matchservice.opponentanalysis.entity.OpponentAnalysisReport;
import com.yukai.team.matchservice.opponentanalysis.entity.OpponentAnalysisStatus;
import com.yukai.team.matchservice.opponentanalysis.exception.OpponentAiException;
import com.yukai.team.matchservice.opponentanalysis.exception.OpponentAnalysisConflictException;
import com.yukai.team.matchservice.opponentanalysis.repository.OpponentAnalysisReportRepository;
import com.yukai.team.matchservice.repository.MatchInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OpponentAnalysisApplicationServiceImpl implements OpponentAnalysisApplicationService {

    private static final String LANGUAGE = "zh-CN";

    private final MatchInfoRepository matchInfoRepository;
    private final MatchOpponentMetricsService metricsService;
    private final OpponentAnalysisInputMapper inputMapper;
    private final OpponentReportGenerator reportGenerator;
    private final OpponentAnalysisReportRepository reportRepository;
    private final OpponentAnalysisReportWriter reportWriter;
    private final OpponentAiProperties aiProperties;
    private final ObjectMapper objectMapper;

    public OpponentAnalysisApplicationServiceImpl(
            MatchInfoRepository matchInfoRepository,
            MatchOpponentMetricsService metricsService,
            OpponentAnalysisInputMapper inputMapper,
            OpponentReportGenerator reportGenerator,
            OpponentAnalysisReportRepository reportRepository,
            OpponentAnalysisReportWriter reportWriter,
            OpponentAiProperties aiProperties,
            ObjectMapper objectMapper
    ) {
        this.matchInfoRepository = matchInfoRepository;
        this.metricsService = metricsService;
        this.inputMapper = inputMapper;
        this.reportGenerator = reportGenerator;
        this.reportRepository = reportRepository;
        this.reportWriter = reportWriter;
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public OpponentAnalysisReportResponse generate(Long matchId, boolean forceRefresh) {
        MatchInfo match = findMatch(matchId);
        validateEligible(match);

        MatchOpponentMetricsResponse metrics = metricsService.getMetrics(matchId, forceRefresh);
        Long snapshotId = metrics.dataSource().snapshotId();
        if (snapshotId == null) {
            throw new OpponentAnalysisConflictException("FLA snapshotId is required to generate opponent analysis report");
        }

        String provider = aiProperties.getProvider();
        String model = aiProperties.getModel();
        String promptVersion = aiProperties.getPromptVersion();

        if (!forceRefresh) {
            var reusable = findReusable(matchId, snapshotId, provider, model, promptVersion);
            if (reusable.isPresent()) {
                return toResponse(reusable.get(), true);
            }
        }

        String metricsJson = writeJson(metrics, "metrics");
        OpponentAnalysisReport pending = reportWriter.createPending(
                matchId,
                match.getOpponentTeamId(),
                snapshotId,
                provider,
                model,
                promptVersion,
                LANGUAGE,
                metricsJson,
                metrics.dataSource().fetchedAt()
        );

        try {
            OpponentAnalysisInput input = inputMapper.toInput(metrics);
            OpponentReportGenerationResult generated = reportGenerator.generateWithMetadata(input);
            String reportJson = writeJson(generated.report(), "AI report");

            OpponentAnalysisReport completed = reportWriter.complete(
                    pending.getId(),
                    reportJson,
                    generated.usage() == null ? null : generated.usage().promptTokens(),
                    generated.usage() == null ? null : generated.usage().completionTokens(),
                    generated.usage() == null ? null : generated.usage().totalTokens(),
                    generated.generatedAt()
            );
            return toResponse(completed, false);
        } catch (OpponentAiException exception) {
            reportWriter.fail(pending.getId(), exception.getCode(), safeMessage(exception.getMessage()));
            throw exception;
        } catch (RuntimeException exception) {
            reportWriter.fail(pending.getId(), "AI_REPORT_GENERATION_FAILED", safeMessage(exception.getMessage()));
            throw exception;
        }
    }

    @Override
    public OpponentAnalysisReportResponse latest(Long matchId) {
        findMatch(matchId);
        OpponentAnalysisReport report = reportRepository
                .findTopByMatchIdAndStatusOrderByCreatedAtDescIdDesc(matchId, OpponentAnalysisStatus.COMPLETED)
                .orElseThrow(() -> new EntityNotFoundException("Completed opponent analysis report not found for match: " + matchId));
        return toResponse(report, false);
    }

    @Override
    public List<OpponentAnalysisReportResponse> history(Long matchId) {
        findMatch(matchId);
        return reportRepository.findByMatchIdOrderByCreatedAtDescIdDesc(matchId).stream()
                .map(report -> toResponse(report, false))
                .toList();
    }

    @Override
    public OpponentAnalysisReportResponse get(Long reportId) {
        OpponentAnalysisReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Opponent analysis report not found: " + reportId));
        return toResponse(report, false);
    }

    private MatchInfo findMatch(Long matchId) {
        return matchInfoRepository.findById(matchId)
                .orElseThrow(() -> new EntityNotFoundException("Match not found: " + matchId));
    }

    private void validateEligible(MatchInfo match) {
        if (match.getMatchStatus() != MatchStatus.SCHEDULED) {
            throw new OpponentAnalysisConflictException(
                    "MATCH_NOT_ELIGIBLE_FOR_OPPONENT_ANALYSIS",
                    "Only scheduled matches can generate pre-match opponent analysis reports"
            );
        }
    }

    private java.util.Optional<OpponentAnalysisReport> findReusable(
            Long matchId,
            Long snapshotId,
            String provider,
            String model,
            String promptVersion
    ) {
        return reportRepository.findTopByMatchIdAndSnapshotIdAndProviderAndModelAndPromptVersionAndLanguageAndStatusOrderByCreatedAtDescIdDesc(
                matchId,
                snapshotId,
                provider,
                model,
                promptVersion,
                LANGUAGE,
                OpponentAnalysisStatus.COMPLETED
        );
    }

    private OpponentAnalysisReportResponse toResponse(OpponentAnalysisReport report, boolean reused) {
        MatchOpponentMetricsResponse metrics = readNullable(report.getMetricsJson(), MatchOpponentMetricsResponse.class, "metrics");
        GeneratedOpponentReport generatedReport = readNullable(report.getReportJson(), GeneratedOpponentReport.class, "AI report");
        String opponentTeamName = metrics == null || metrics.match() == null ? null : metrics.match().opponentTeamName();
        return new OpponentAnalysisReportResponse(
                report.getId(),
                report.getMatchId(),
                report.getOpponentTeamId(),
                opponentTeamName,
                report.getStatus(),
                report.getProvider(),
                report.getModel(),
                report.getPromptVersion(),
                report.getLanguage(),
                reused,
                report.getSnapshotId(),
                report.getSourceFetchedAt(),
                report.getGeneratedAt(),
                report.getCreatedAt(),
                metrics,
                generatedReport,
                new OpponentAnalysisReportResponse.Usage(
                        report.getPromptTokens(),
                        report.getCompletionTokens(),
                        report.getTotalTokens()
                ),
                report.getStatus() == OpponentAnalysisStatus.FAILED ? report.getErrorCode() : null
        );
    }

    private String writeJson(Object value, String label) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new OpponentAiException(HttpStatus.INTERNAL_SERVER_ERROR, "AI_REPORT_SERIALIZATION_FAILED", "Failed to serialize " + label, exception);
        }
    }

    private <T> T readNullable(String json, Class<T> type, String label) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new OpponentAiException(HttpStatus.INTERNAL_SERVER_ERROR, "AI_REPORT_DESERIALIZATION_FAILED", "Failed to deserialize " + label, exception);
        }
    }

    private String safeMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Opponent analysis report generation failed";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
