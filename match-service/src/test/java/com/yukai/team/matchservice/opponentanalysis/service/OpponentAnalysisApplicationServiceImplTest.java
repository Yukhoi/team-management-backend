package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.config.JacksonConfig;
import com.yukai.team.matchservice.entity.HomeAway;
import com.yukai.team.matchservice.entity.MatchInfo;
import com.yukai.team.matchservice.entity.MatchStatus;
import com.yukai.team.matchservice.opponentanalysis.config.OpponentAiProperties;
import com.yukai.team.matchservice.opponentanalysis.dto.LeaguePerformanceMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.MatchOpponentMetricsResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamComparisonMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamPerformanceMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.GeneratedOpponentReport;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.OpponentReportGenerationResult;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.ThreatLevel;
import com.yukai.team.matchservice.opponentanalysis.entity.OpponentAnalysisReport;
import com.yukai.team.matchservice.opponentanalysis.entity.OpponentAnalysisStatus;
import com.yukai.team.matchservice.opponentanalysis.exception.OpponentAiException;
import com.yukai.team.matchservice.opponentanalysis.exception.OpponentAnalysisConflictException;
import com.yukai.team.matchservice.opponentanalysis.repository.OpponentAnalysisReportRepository;
import com.yukai.team.matchservice.repository.MatchInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpponentAnalysisApplicationServiceImplTest {

    private MatchInfoRepository matchInfoRepository;
    private MatchOpponentMetricsService metricsService;
    private OpponentReportGenerator reportGenerator;
    private OpponentAnalysisReportRepository reportRepository;
    private OpponentAnalysisReportWriter reportWriter;
    private OpponentAnalysisApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        matchInfoRepository = mock(MatchInfoRepository.class);
        metricsService = mock(MatchOpponentMetricsService.class);
        reportGenerator = mock(OpponentReportGenerator.class);
        reportRepository = mock(OpponentAnalysisReportRepository.class);
        reportWriter = mock(OpponentAnalysisReportWriter.class);
        OpponentAiProperties properties = new OpponentAiProperties();
        properties.setProvider("openrouter");
        properties.setModel("test-model");
        properties.setPromptVersion("v1");
        service = new OpponentAnalysisApplicationServiceImpl(
                matchInfoRepository,
                metricsService,
                new OpponentAnalysisInputMapper(),
                reportGenerator,
                reportRepository,
                reportWriter,
                properties,
                new JacksonConfig().objectMapper()
        );
    }

    @Test
    void generatesAndCompletesReport() {
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(match(MatchStatus.SCHEDULED)));
        when(metricsService.getMetrics(42L, false)).thenReturn(metrics(10L));
        when(reportRepository.findTopByMatchIdAndSnapshotIdAndProviderAndModelAndPromptVersionAndLanguageAndStatusOrderByCreatedAtDescIdDesc(42L, 10L, "openrouter", "test-model", "v1", "zh-CN", OpponentAnalysisStatus.COMPLETED))
                .thenReturn(Optional.empty());
        when(reportWriter.createPending(eq(42L), eq(13L), eq(10L), eq("openrouter"), eq("test-model"), eq("v1"), eq("zh-CN"), any(), any()))
                .thenReturn(report(1L, OpponentAnalysisStatus.PENDING, 10L));
        when(reportGenerator.generateWithMetadata(any())).thenReturn(generated());
        when(reportWriter.complete(eq(1L), any(), eq(11), eq(22), eq(33), any()))
                .thenReturn(completedReport(1L, 10L));

        var response = service.generate(42L, false);

        assertThat(response.status()).isEqualTo(OpponentAnalysisStatus.COMPLETED);
        assertThat(response.reused()).isFalse();
        assertThat(response.usage().promptTokens()).isEqualTo(11);
        verify(reportGenerator).generateWithMetadata(any());
        verify(reportWriter).complete(eq(1L), any(), eq(11), eq(22), eq(33), any());
    }

    @Test
    void reusesCompletedReportWhenAllReuseKeysMatchAndForceRefreshIsFalse() {
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(match(MatchStatus.SCHEDULED)));
        when(metricsService.getMetrics(42L, false)).thenReturn(metrics(10L));
        when(reportRepository.findTopByMatchIdAndSnapshotIdAndProviderAndModelAndPromptVersionAndLanguageAndStatusOrderByCreatedAtDescIdDesc(42L, 10L, "openrouter", "test-model", "v1", "zh-CN", OpponentAnalysisStatus.COMPLETED))
                .thenReturn(Optional.of(completedReport(7L, 10L)));

        var response = service.generate(42L, false);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.reused()).isTrue();
        verify(reportGenerator, never()).generateWithMetadata(any());
        verify(reportWriter, never()).createPending(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void forceRefreshSkipsReuseAndRegenerates() {
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(match(MatchStatus.SCHEDULED)));
        when(metricsService.getMetrics(42L, true)).thenReturn(metrics(11L));
        when(reportWriter.createPending(eq(42L), eq(13L), eq(11L), any(), any(), any(), any(), any(), any()))
                .thenReturn(report(2L, OpponentAnalysisStatus.PENDING, 11L));
        when(reportGenerator.generateWithMetadata(any())).thenReturn(generated());
        when(reportWriter.complete(eq(2L), any(), any(), any(), any(), any()))
                .thenReturn(completedReport(2L, 11L));

        var response = service.generate(42L, true);

        assertThat(response.snapshotId()).isEqualTo(11L);
        verify(reportRepository, never()).findTopByMatchIdAndSnapshotIdAndProviderAndModelAndPromptVersionAndLanguageAndStatusOrderByCreatedAtDescIdDesc(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void missingMatchReturnsNotFound() {
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generate(42L, false))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void finishedMatchIsRejected() {
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(match(MatchStatus.FINISHED)));

        assertThatThrownBy(() -> service.generate(42L, false))
                .isInstanceOf(OpponentAnalysisConflictException.class)
                .extracting("code")
                .isEqualTo("MATCH_NOT_ELIGIBLE_FOR_OPPONENT_ANALYSIS");
    }

    @Test
    void aiFailureMarksReportFailedAndPropagatesStatus() {
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(match(MatchStatus.SCHEDULED)));
        when(metricsService.getMetrics(42L, false)).thenReturn(metrics(10L));
        when(reportRepository.findTopByMatchIdAndSnapshotIdAndProviderAndModelAndPromptVersionAndLanguageAndStatusOrderByCreatedAtDescIdDesc(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(reportWriter.createPending(eq(42L), eq(13L), eq(10L), any(), any(), any(), any(), any(), any()))
                .thenReturn(report(3L, OpponentAnalysisStatus.PENDING, 10L));
        OpponentAiException exception = new OpponentAiException(HttpStatus.TOO_MANY_REQUESTS, "AI_RATE_LIMITED", "rate limited");
        doThrow(exception).when(reportGenerator).generateWithMetadata(any());

        assertThatThrownBy(() -> service.generate(42L, false))
                .isSameAs(exception);
        verify(reportWriter).fail(3L, "AI_RATE_LIMITED", "rate limited");
    }

    @Test
    void latestReturnsLatestCompletedReport() {
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(match(MatchStatus.SCHEDULED)));
        when(reportRepository.findTopByMatchIdAndStatusOrderByCreatedAtDescIdDesc(42L, OpponentAnalysisStatus.COMPLETED))
                .thenReturn(Optional.of(completedReport(4L, 10L)));

        assertThat(service.latest(42L).id()).isEqualTo(4L);
    }

    @Test
    void historyReturnsReportsInRepositoryOrder() {
        when(matchInfoRepository.findById(42L)).thenReturn(Optional.of(match(MatchStatus.SCHEDULED)));
        when(reportRepository.findByMatchIdOrderByCreatedAtDescIdDesc(42L))
                .thenReturn(List.of(completedReport(5L, 10L), report(6L, OpponentAnalysisStatus.FAILED, 10L)));

        assertThat(service.history(42L)).extracting("id").containsExactly(5L, 6L);
    }

    private MatchInfo match(MatchStatus status) {
        MatchInfo match = new MatchInfo();
        match.setId(42L);
        match.setOpponentTeamId(13L);
        match.setMatchStatus(status);
        return match;
    }

    private MatchOpponentMetricsResponse metrics(Long snapshotId) {
        return new MatchOpponentMetricsResponse(
                new MatchOpponentMetricsResponse.MatchContext(42L, 5L, "Paris League", "2026", 12L, "YEXIAO PARIS FC", 13L, "ASTERIA", OffsetDateTime.parse("2026-08-10T18:30:00Z"), HomeAway.HOME, MatchStatus.SCHEDULED),
                team(12L, "YEXIAO PARIS FC"),
                team(13L, "ASTERIA"),
                new LeaguePerformanceMetrics(14, null, BigDecimal.TEN, new BigDecimal("1.35"), null, null, new BigDecimal("2.58"), new BigDecimal("2.58"), 112, 40, 68, new BigDecimal("0.42"), BigDecimal.ZERO),
                new TeamComparisonMetrics(-48, 9, new BigDecimal("-0.62"), BigDecimal.ZERO, new BigDecimal("-1.89"), new BigDecimal("3.61"), -143, -2, new BigDecimal("-0.84"), new BigDecimal("0.88")),
                new MatchOpponentMetricsResponse.DataSource("FLA", 1365L, 15L, snapshotId, "hash", OffsetDateTime.parse("2026-08-05T12:00:00Z"), CacheStatus.HIT, 21600L, false, "LATEST_TO_OLDEST", "CALCULATED")
        );
    }

    private TeamPerformanceMetrics team(Long id, String name) {
        return new TeamPerformanceMetrics(id, id + 1000, name, 1, 20, 10, 6, 2, 2, new BigDecimal("0.60"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN, 20, 10, 10, new BigDecimal("2.00"), BigDecimal.ONE, 0, 0, List.of("won", "draw", "lost"), List.<String>of(), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private OpponentReportGenerationResult generated() {
        return new OpponentReportGenerationResult("openrouter", "test-model", "v1", generatedReport(), new OpponentReportGenerationResult.Usage(11, 22, 33), OffsetDateTime.parse("2026-08-06T10:00:00Z"));
    }

    private GeneratedOpponentReport generatedReport() {
        return new GeneratedOpponentReport(ThreatLevel.MEDIUM, "对手具备一定威胁。", List.of(), List.of(), List.of(), List.of("压缩空间"), List.of("无球员级别数据"));
    }

    private OpponentAnalysisReport completedReport(Long id, Long snapshotId) {
        OpponentAnalysisReport report = report(id, OpponentAnalysisStatus.COMPLETED, snapshotId);
        report.setReportJson("{\"threatLevel\":\"MEDIUM\",\"summary\":\"对手具备一定威胁。\",\"comparison\":[],\"strengths\":[],\"weaknesses\":[],\"recommendations\":[\"压缩空间\"],\"dataLimitations\":[]}");
        report.setPromptTokens(11);
        report.setCompletionTokens(22);
        report.setTotalTokens(33);
        report.setGeneratedAt(OffsetDateTime.parse("2026-08-06T10:00:00Z"));
        return report;
    }

    private OpponentAnalysisReport report(Long id, OpponentAnalysisStatus status, Long snapshotId) {
        OpponentAnalysisReport report = new OpponentAnalysisReport();
        report.setId(id);
        report.setMatchId(42L);
        report.setOpponentTeamId(13L);
        report.setSnapshotId(snapshotId);
        report.setProvider("openrouter");
        report.setModel("test-model");
        report.setPromptVersion("v1");
        report.setLanguage("zh-CN");
        report.setStatus(status);
        report.setMetricsJson("{}");
        report.setCreatedAt(OffsetDateTime.parse("2026-08-06T09:00:00Z"));
        return report;
    }
}
