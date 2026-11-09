package com.yukai.team.matchservice.opponentanalysis.repository;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yukai.team.matchservice.config.JacksonConfig;
import com.yukai.team.matchservice.context.CurrentUser;
import com.yukai.team.matchservice.context.UserContextHolder;
import com.yukai.team.matchservice.entity.MatchInfo;
import com.yukai.team.matchservice.entity.MatchStatus;
import com.yukai.team.matchservice.opponentanalysis.config.OpponentAiProperties;
import com.yukai.team.matchservice.opponentanalysis.controller.OpponentAnalysisReportController;
import com.yukai.team.matchservice.opponentanalysis.dto.MatchOpponentMetricsResponse;
import com.yukai.team.matchservice.opponentanalysis.entity.OpponentAnalysisReport;
import com.yukai.team.matchservice.opponentanalysis.entity.OpponentAnalysisStatus;
import com.yukai.team.matchservice.opponentanalysis.service.MatchOpponentMetricsService;
import com.yukai.team.matchservice.opponentanalysis.service.OpenRouterOpponentReportGenerator;
import com.yukai.team.matchservice.opponentanalysis.service.OpponentAnalysisApplicationServiceImpl;
import com.yukai.team.matchservice.opponentanalysis.service.OpponentAnalysisInputMapper;
import com.yukai.team.matchservice.opponentanalysis.service.OpponentAnalysisReportWriterImpl;
import com.yukai.team.matchservice.opponentanalysis.service.PromptLoader;
import com.yukai.team.matchservice.opponentanalysis.service.RecentFormAiFixture;
import com.yukai.team.matchservice.repository.MatchInfoRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:opponent-analysis-report;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE SCHEMA IF NOT EXISTS match",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.default_schema=match"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OpponentAnalysisReportRepositoryTest.JpaTestConfig.class)
class OpponentAnalysisReportRepositoryTest {

    @Autowired
    private OpponentAnalysisReportRepository repository;

    @Test
    void findsLatestCompletedReportOnly() {
        repository.save(report(1L, 10L, OpponentAnalysisStatus.COMPLETED, "model-a", OffsetDateTime.parse("2026-08-06T09:00:00Z")));
        repository.save(report(1L, 10L, OpponentAnalysisStatus.FAILED, "model-a", OffsetDateTime.parse("2026-08-06T11:00:00Z")));
        repository.save(report(1L, 10L, OpponentAnalysisStatus.COMPLETED, "model-a", OffsetDateTime.parse("2026-08-06T10:00:00Z")));

        var latest = repository.findTopByMatchIdAndStatusOrderByCreatedAtDescIdDesc(1L, OpponentAnalysisStatus.COMPLETED);

        assertThat(latest).isPresent();
        assertThat(latest.get().getCreatedAt()).isEqualTo(OffsetDateTime.parse("2026-08-06T10:00:00Z"));
    }

    @Test
    void findsHistoryByCreatedAtDesc() {
        repository.save(report(1L, 10L, OpponentAnalysisStatus.COMPLETED, "model-a", OffsetDateTime.parse("2026-08-06T09:00:00Z")));
        repository.save(report(1L, 11L, OpponentAnalysisStatus.COMPLETED, "model-a", OffsetDateTime.parse("2026-08-06T10:00:00Z")));

        assertThat(repository.findByMatchIdOrderByCreatedAtDescIdDesc(1L))
                .extracting(OpponentAnalysisReport::getSnapshotId)
                .containsExactly(11L, 10L);
    }

    @Test
    void preciseReuseQueryRequiresCompletedStatusAndSameKeys() {
        repository.save(report(1L, 10L, OpponentAnalysisStatus.FAILED, "model-a", OffsetDateTime.parse("2026-08-06T09:00:00Z")));
        repository.save(report(1L, 11L, OpponentAnalysisStatus.COMPLETED, "model-a", OffsetDateTime.parse("2026-08-06T10:00:00Z")));
        repository.save(report(1L, 10L, OpponentAnalysisStatus.COMPLETED, "model-b", OffsetDateTime.parse("2026-08-06T11:00:00Z")));
        repository.save(report(1L, 10L, OpponentAnalysisStatus.COMPLETED, "model-a", OffsetDateTime.parse("2026-08-06T12:00:00Z")));

        var reusable = repository.findTopByMatchIdAndMetricsInputFingerprintAndProviderAndModelAndPromptVersionAndLanguageAndStatusOrderByCreatedAtDescIdDesc(
                1L,
                "a".repeat(64),
                "openrouter",
                "model-a",
                "v1",
                "zh-CN",
                OpponentAnalysisStatus.COMPLETED
        );

        assertThat(reusable).isPresent();
        assertThat(reusable.get().getModel()).isEqualTo("model-a");
        assertThat(reusable.get().getSnapshotId()).isEqualTo(10L);
        assertThat(reusable.get().getStatus()).isEqualTo(OpponentAnalysisStatus.COMPLETED);
    }

    @Test
    void differentSnapshotsCanBeSavedAsDifferentVersions() {
        repository.saveAndFlush(report(1L, 10L, OpponentAnalysisStatus.COMPLETED, "model-a", OffsetDateTime.parse("2026-08-06T09:00:00Z")));
        repository.saveAndFlush(report(1L, 11L, OpponentAnalysisStatus.COMPLETED, "model-a", OffsetDateTime.parse("2026-08-06T10:00:00Z")));

        assertThat(repository.findAll()).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"fingerprint", "provider", "model", "language", "prompt", "legacy", "failed"})
    void incompatibleReportsNeverParticipateInExactReuse(String dimension) {
        var row = report(1L, 10L, OpponentAnalysisStatus.COMPLETED, "model-a", OffsetDateTime.now());
        switch (dimension) {
            case "fingerprint" -> row.setMetricsInputFingerprint("b".repeat(64));
            case "provider" -> row.setProvider("other");
            case "model" -> row.setModel("other");
            case "language" -> row.setLanguage("en-US");
            case "prompt" -> row.setPromptVersion("v2");
            case "legacy" -> row.setMetricsInputFingerprint(null);
            case "failed" -> row.setStatus(OpponentAnalysisStatus.FAILED);
        }
        repository.saveAndFlush(row);
        assertThat(repository.findTopByMatchIdAndMetricsInputFingerprintAndProviderAndModelAndPromptVersionAndLanguageAndStatusOrderByCreatedAtDescIdDesc(
                1L, "a".repeat(64), "openrouter", "model-a", "v1", "zh-CN", OpponentAnalysisStatus.COMPLETED)).isEmpty();
        assertThat(repository.findByMatchIdOrderByCreatedAtDescIdDesc(1L)).hasSize(1);
    }

    @Test
    void identicalFingerprintUsesIdAsTieBreakerWithoutOverwritingHistory() {
        var time = OffsetDateTime.parse("2026-08-06T09:00:00Z");
        repository.saveAndFlush(report(1L, 10L, OpponentAnalysisStatus.COMPLETED, "model-a", time));
        var newest = repository.saveAndFlush(report(1L, 10L, OpponentAnalysisStatus.COMPLETED, "model-a", time));
        var reused = repository.findTopByMatchIdAndMetricsInputFingerprintAndProviderAndModelAndPromptVersionAndLanguageAndStatusOrderByCreatedAtDescIdDesc(
                1L, "a".repeat(64), "openrouter", "model-a", "v1", "zh-CN", OpponentAnalysisStatus.COMPLETED);
        assertThat(reused).hasValueSatisfying(row -> assertThat(row.getId()).isEqualTo(newest.getId()));
        assertThat(repository.findByMatchIdOrderByCreatedAtDescIdDesc(1L)).hasSize(2);
    }

    @Test
    void matchElevenPostUsesRealMapperGeneratorAndPersistenceWithMockLlm() throws Exception {
        var json = new JacksonConfig().objectMapper();
        var matchRepository = Mockito.mock(MatchInfoRepository.class);
        var metricsService = Mockito.mock(MatchOpponentMetricsService.class);
        var match = new MatchInfo();
        match.setId(11L);
        match.setOpponentTeamId(13L);
        match.setMatchStatus(MatchStatus.SCHEDULED);
        Mockito.when(matchRepository.findById(11L)).thenReturn(Optional.of(match));
        var metrics = RecentFormAiFixture.metrics();
        Mockito.when(metricsService.getMetrics(ArgumentMatchers.eq(11L), ArgumentMatchers.anyBoolean()))
                .thenReturn(metrics);
        var properties = new OpponentAiProperties();
        properties.setModel("mock-model");
        properties.setApiKey("mock-key");
        var builder = RestClient.builder().baseUrl("https://openrouter.test");
        var server = MockRestServiceServer.bindTo(builder).build();
        var reportBody = json.createObjectNode().put("threatLevel", "LOW").put("summary", "我方五场四胜一负，对手五场全败，仍需认真备战。");
        for (String field : new String[]{"comparison", "strengths", "weaknesses", "recommendations", "dataLimitations"}) reportBody.putArray(field);
        String llmResponse = json.writeValueAsString(Map.of("choices", List.of(Map.of(
                "message", Map.of("content", json.writeValueAsString(reportBody)), "finish_reason", "stop"))));
        server.expect(ExpectedCount.times(6),
                MockRestRequestMatchers.requestTo("https://openrouter.test/chat/completions"))
                .andExpect(request -> {
                    var body = json.readTree(((MockClientHttpRequest) request).getBodyAsString());
                    var input = json.readTree(body.at("/messages/1/content").asText());
                    if (input.at("/ourTeam/recentForm/recentFormAvailable").asBoolean())
                        assertThat(input.at("/ourTeam/recentForm/recentFive/wins").asInt()).isEqualTo(4);
                    assertThat(input.at("/opponent/recentForm/recentFive/goalsAgainst").asInt()).isEqualTo(16);
                    assertThat(input.at("/ourTeam/identity/flaTeamId").asLong()).isEqualTo(270L);
                })
                .andRespond(MockRestResponseCreators.withSuccess(llmResponse, MediaType.APPLICATION_JSON));
        var generator = new OpenRouterOpponentReportGenerator(builder.build(), properties,
                new PromptLoader(), json);
        var service = new OpponentAnalysisApplicationServiceImpl(
                matchRepository, metricsService, new OpponentAnalysisInputMapper(), generator,
                repository, new OpponentAnalysisReportWriterImpl(repository), properties, json);
        var mvc = MockMvcBuilders.standaloneSetup(
                new OpponentAnalysisReportController(service)).build();
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));
        try {
            mvc.perform(MockMvcRequestBuilders.post("/api/v1/matches/11/opponent-analysis?forceRefresh=true"))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.reused").value(false));
            var first = repository.findByMatchIdOrderByCreatedAtDescIdDesc(11L).get(0);
            assertThat(first.getMetricsInputFingerprint()).hasSize(64);
            assertThat(first.getAiInputJson()).contains("recentFormSampleSize", "YEXIAO PARIS FC");
            mvc.perform(MockMvcRequestBuilders.post("/api/v1/matches/11/opponent-analysis?forceRefresh=false"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.reused").value(true))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.id").value(first.getId()));
            // Each variation keeps the original standings snapshot unless explicitly changed.
            for (String dimension : new String[]{"recent", "standings", "unavailable", "recovery"}) {
                ObjectNode changed = json.valueToTree(metrics);
                if (dimension.equals("recent")) {
                    ((ObjectNode) changed.at("/ourTeam/recentForm/recentFive")).put("goalsFor", 28);
                    ((ObjectNode) changed.at("/ourTeam/recentForm/source")).put("versionId", "updated-score");
                } else if (dimension.equals("standings")) {
                    ((ObjectNode) changed.get("dataSource")).put("snapshotId", 999);
                } else {
                    ((ObjectNode) changed.at("/ourTeam/recentForm")).put("status", dimension.equals("unavailable") ? "SOURCE_UNAVAILABLE" : "AVAILABLE");
                    ((ObjectNode) changed.at("/ourTeam/recentForm/source")).put("versionId", "recovered-source");
                }
                Mockito.when(metricsService.getMetrics(11L, false)).thenReturn(json.convertValue(changed, MatchOpponentMetricsResponse.class));
                mvc.perform(MockMvcRequestBuilders.post("/api/v1/matches/11/opponent-analysis?forceRefresh=false"))
                        .andExpect(MockMvcResultMatchers.jsonPath("$.reused").value(false));
            }
            // Force refresh bypasses an existing completed report for exactly the same input.
            service.generate(11L, true);
            assertThat(repository.findByMatchIdOrderByCreatedAtDescIdDesc(11L)).hasSize(6);
            Mockito.verify(metricsService, Mockito.times(2)).getMetrics(11L, true);
            server.verify();
        } finally {
            UserContextHolder.clear();
        }
    }

    @Autowired
    private javax.sql.DataSource dataSource;

    @Test
    void fingerprintMigrationCanBeAppliedTwice() {
        var path = java.nio.file.Path.of("sql/alter-opponent-analysis-report-input-fingerprint.sql");
        if (!java.nio.file.Files.exists(path)) {
            path = java.nio.file.Path.of("match-service").resolve(path);
        }
        var script = new org.springframework.jdbc.datasource.init.ResourceDatabasePopulator(
                new org.springframework.core.io.FileSystemResource(path));
        script.execute(dataSource);
        script.execute(dataSource);
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(dataSource);
        assertThat(jdbc.queryForList("SELECT metrics_input_fingerprint, ai_input_json FROM match.opponent_analysis_report WHERE 1=0")).isEmpty();
    }

    private OpponentAnalysisReport report(Long matchId, Long snapshotId, OpponentAnalysisStatus status, String model, OffsetDateTime createdAt) {
        OpponentAnalysisReport report = new OpponentAnalysisReport();
        report.setMatchId(matchId);
        report.setOpponentTeamId(13L);
        report.setSnapshotId(snapshotId);
        report.setMetricsInputFingerprint("a".repeat(64));
        report.setProvider("openrouter");
        report.setModel(model);
        report.setPromptVersion("v1");
        report.setLanguage("zh-CN");
        report.setStatus(status);
        report.setMetricsJson("{}");
        report.setReportJson(status == OpponentAnalysisStatus.COMPLETED ? "{\"threatLevel\":\"LOW\",\"summary\":\"测试\",\"comparison\":[],\"strengths\":[],\"weaknesses\":[],\"recommendations\":[],\"dataLimitations\":[]}" : null);
        report.setCreatedAt(createdAt);
        report.setUpdatedAt(createdAt);
        return report;
    }

    @TestConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackages = "com.yukai.team.matchservice.opponentanalysis.entity")
    @EnableJpaRepositories(basePackageClasses = OpponentAnalysisReportRepository.class)
    static class JpaTestConfig {
    }
}
