package com.yukai.team.matchservice.opponentanalysis.repository;

import com.yukai.team.matchservice.opponentanalysis.entity.OpponentAnalysisReport;
import com.yukai.team.matchservice.opponentanalysis.entity.OpponentAnalysisStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.OffsetDateTime;

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

        var reusable = repository.findTopByMatchIdAndSnapshotIdAndProviderAndModelAndPromptVersionAndLanguageAndStatusOrderByCreatedAtDescIdDesc(
                1L,
                10L,
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

    private OpponentAnalysisReport report(Long matchId, Long snapshotId, OpponentAnalysisStatus status, String model, OffsetDateTime createdAt) {
        OpponentAnalysisReport report = new OpponentAnalysisReport();
        report.setMatchId(matchId);
        report.setOpponentTeamId(13L);
        report.setSnapshotId(snapshotId);
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
