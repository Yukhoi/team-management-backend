package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.entity.OpponentAnalysisReport;
import com.yukai.team.matchservice.opponentanalysis.entity.OpponentAnalysisStatus;
import com.yukai.team.matchservice.opponentanalysis.repository.OpponentAnalysisReportRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class OpponentAnalysisReportWriterImpl implements OpponentAnalysisReportWriter {

    private final OpponentAnalysisReportRepository repository;

    public OpponentAnalysisReportWriterImpl(OpponentAnalysisReportRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OpponentAnalysisReport createPending(
            Long matchId,
            Long opponentTeamId,
            Long snapshotId,
            String provider,
            String model,
            String promptVersion,
            String language,
            String metricsJson,
            OffsetDateTime sourceFetchedAt
    ) {
        OpponentAnalysisReport report = new OpponentAnalysisReport();
        report.setMatchId(matchId);
        report.setOpponentTeamId(opponentTeamId);
        report.setSnapshotId(snapshotId);
        report.setProvider(provider);
        report.setModel(model);
        report.setPromptVersion(promptVersion);
        report.setLanguage(language);
        report.setStatus(OpponentAnalysisStatus.PENDING);
        report.setMetricsJson(metricsJson);
        report.setSourceFetchedAt(sourceFetchedAt);
        return repository.saveAndFlush(report);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OpponentAnalysisReport complete(
            Long reportId,
            String reportJson,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            OffsetDateTime generatedAt
    ) {
        OpponentAnalysisReport report = find(reportId);
        report.setStatus(OpponentAnalysisStatus.COMPLETED);
        report.setReportJson(reportJson);
        report.setErrorCode(null);
        report.setErrorMessage(null);
        report.setPromptTokens(promptTokens);
        report.setCompletionTokens(completionTokens);
        report.setTotalTokens(totalTokens);
        report.setGeneratedAt(generatedAt == null ? OffsetDateTime.now() : generatedAt);
        return repository.saveAndFlush(report);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OpponentAnalysisReport fail(Long reportId, String errorCode, String errorMessage) {
        OpponentAnalysisReport report = find(reportId);
        report.setStatus(OpponentAnalysisStatus.FAILED);
        report.setErrorCode(errorCode);
        report.setErrorMessage(errorMessage);
        report.setReportJson(null);
        return repository.saveAndFlush(report);
    }

    private OpponentAnalysisReport find(Long reportId) {
        return repository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Opponent analysis report not found: " + reportId));
    }
}
