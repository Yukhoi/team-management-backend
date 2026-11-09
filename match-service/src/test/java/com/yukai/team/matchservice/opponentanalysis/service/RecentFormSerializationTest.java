package com.yukai.team.matchservice.opponentanalysis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.MatchOpponentMetricsResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.RecentFormMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamComparisonMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamPerformanceMetrics;
import io.swagger.v3.core.converter.ModelConverters;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecentFormSerializationTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void availabilityNullNumbersAndExistingResultCodesRoundTripAndLegacyReportsStillRead() throws Exception {
        var calculator = new OpponentMetricsCalculator();
        var team = calculator.calculateTeamMetrics(12L, 270L, new FlaStandingEntryResponse(), 1,
                RecentFormMetrics.unavailable(RecentFormMetrics.Status.SOURCE_UNAVAILABLE, null));
        String json = mapper.writeValueAsString(team);
        var tree = mapper.readTree(json);
        assertThat(tree.get("recentFormAvailable").asBoolean()).isFalse();
        assertThat(tree.get("recentFormSampleSize").isNull()).isTrue();
        assertThat(tree.get("recentFiveWins").isNull()).isTrue();
        assertThat(tree.get("recentFive").isArray()).isTrue();
        assertThat(mapper.readValue(json, TeamPerformanceMetrics.class)).isEqualTo(team);
        assertThat(tree.at("/recentForm/competitionScope").asText()).isEqualTo("ALL_COMPETITIONS");
        assertThat(tree.at("/recentForm/seasonScope").asText()).isEqualTo("CROSS_SEASON_ALLOWED");
        var oldForm = (com.fasterxml.jackson.databind.node.ObjectNode) tree.get("recentForm").deepCopy();
        oldForm.remove(java.util.List.of("competitionScope", "seasonScope"));
        assertThat(mapper.treeToValue(oldForm, RecentFormMetrics.class).competitionScope()).isNull();
        assertThat(mapper.treeToValue(oldForm, RecentFormMetrics.class).seasonScope()).isNull();
        var legacy = (com.fasterxml.jackson.databind.node.ObjectNode) tree;
        legacy.remove(java.util.List.of("recentForm", "recentFormAvailable", "recentFormSampleSize"));
        assertThat(mapper.treeToValue(legacy, TeamPerformanceMetrics.class).recentForm()).isNull();
        assertThat(mapper.treeToValue(legacy, TeamPerformanceMetrics.class).recentFive()).isEmpty();
        var comparison = calculator.compare(team, team);
        assertThat(mapper.readTree(mapper.writeValueAsString(comparison)).get("recentFormComparisonAvailable").asBoolean()).isFalse();
        assertThat(mapper.readValue(mapper.writeValueAsString(comparison), TeamComparisonMetrics.class)).isEqualTo(comparison);
    }

    @Test
    void standingsTimeScopeAndFingerprintCachePolicySurviveSavedMetricsRoundTrip() throws Exception {
        var source = new MatchOpponentMetricsResponse.DataSource("FLA", 16L, 2L, 1L, "h",
                RecentFormCalculatorTest.FETCHED, CacheStatus.HIT, 21600L, false, "LATEST_TO_OLDEST", "CALCULATED");
        assertThat(source.standingsTemporalScope()).isEqualTo("FETCH_TIME_SNAPSHOT_NOT_HISTORICAL_PREMATCH");
        assertThat(source.reportCacheCompatibility()).isEqualTo("METRICS_INPUT_FINGERPRINT");
        assertThat(mapper.readValue(mapper.writeValueAsString(source), MatchOpponentMetricsResponse.DataSource.class)).isEqualTo(source);
    }

    @Test
    void openApiPublishesAvailabilityAndIndependentSourceSchema() {
        var schemas = ModelConverters.getInstance().readAll(TeamPerformanceMetrics.class);
        var properties = schemas.get("TeamPerformanceMetrics").getProperties();
        assertThat(properties).containsKeys("recentForm", "recentFormAvailable", "recentFormSampleSize");
        assertThat(schemas.keySet()).contains("RecentFormMetrics");
    }
}
