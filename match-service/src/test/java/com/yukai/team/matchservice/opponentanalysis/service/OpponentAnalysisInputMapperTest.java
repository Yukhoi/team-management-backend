package com.yukai.team.matchservice.opponentanalysis.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.yukai.team.matchservice.config.JacksonConfig;
import com.yukai.team.matchservice.entity.HomeAway;
import com.yukai.team.matchservice.entity.MatchStatus;
import com.yukai.team.matchservice.opponentanalysis.dto.LeaguePerformanceMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.MatchOpponentMetricsResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamComparisonMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamPerformanceMetrics;
import com.yukai.team.matchservice.opponentanalysis.service.CacheStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpponentAnalysisInputMapperTest {

    private final OpponentAnalysisInputMapper mapper = new OpponentAnalysisInputMapper();

    @Test
    void mapsMetricsToAiInputWithoutRawPayload() throws JsonProcessingException {
        var metrics = metrics();

        var input = mapper.toInput(metrics);

        assertThat(input.match().competition()).isEqualTo("Paris League");
        assertThat(input.match().season()).isEqualTo("2026");
        assertThat(input.match().homeAway()).isEqualTo(HomeAway.HOME);
        assertThat(input.ourTeam().name()).isEqualTo("YEXIAO PARIS FC");
        assertThat(input.ourTeam().recentFive()).isEmpty();
        assertThat(input.ourTeam().recentForm().recentFormStatus()).isEqualTo("SOURCE_UNAVAILABLE");
        assertThat(input.opponent().name()).isEqualTo("ASTERIA");
        assertThat(input.leagueAverage().averageGoalsForPerGame()).isEqualByComparingTo("2.58");
        assertThat(input.leagueAverage().averageGoalsAgainstPerGame()).isEqualByComparingTo("2.58");
        assertThat(input.leagueAverage().averagePointsPerGame()).isEqualByComparingTo("1.35");
        assertThat(input.leagueAverage().averageWinRate()).isEqualByComparingTo("0.42");
        assertThat(input.comparison().pointsDifference()).isEqualTo(-48);
        assertThat(input.dataLimitations()).containsExactly(
                "无球员级别数据",
                "无阵型数据",
                "无射门和控球数据",
                "无伤病和停赛数据",
                "无可靠历史交锋数据"
        );
        String json = new JacksonConfig().objectMapper().writeValueAsString(input);
        assertThat(json).doesNotContain("rawPayload", "fetchedAt", "cacheStatus");
        assertThat(input.provenance().standingsPayloadHash()).isEqualTo("hash");
    }

    @Test
    void mapsMatchElevenRecentFormAndIdentityWithoutSwappingRoles() {
        var input = mapper.toInput(RecentFormAiFixture.metrics());
        var ours = input.ourTeam();
        var opponent = input.opponent();
        assertThat(input.provenance().matchId()).isEqualTo(11L);
        assertThat(ours.identity().internalTeamId()).isEqualTo(12L);
        assertThat(ours.identity().internalTeamName()).isEqualTo("Coach Docker Team");
        assertThat(ours.identity().flaTeamId()).isEqualTo(270L);
        assertThat(ours.identity().flaTeamName()).isEqualTo("YEXIAO PARIS FC");
        assertThat(ours.identity().role()).isEqualTo("OUR_TEAM");
        assertThat(opponent.identity().internalTeamName()).isEqualTo("FLA Test Team 1");
        assertThat(opponent.identity().flaTeamId()).isEqualTo(204L);
        assertThat(opponent.identity().role()).isEqualTo("OPPONENT");
        assertThat(ours.recentFive()).containsExactly("lost", "won", "won", "won", "won");
        assertThat(ours.recentForm().recentFormSampleSize()).isEqualTo(6);
        assertThat(ours.recentForm().recentFive()).isEqualTo(new com.yukai.team.matchservice.opponentanalysis.dto.RecentFormMetrics.Window(5, 4, 0, 1, 27, 8, new BigDecimal("0.8")));
        assertThat(opponent.recentForm().recentFive().wins()).isZero();
        assertThat(opponent.recentForm().recentFive().losses()).isEqualTo(5);
        assertThat(opponent.recentForm().recentFive().goalsFor()).isEqualTo(6);
        assertThat(opponent.recentForm().recentFive().goalsAgainst()).isEqualTo(16);
        assertThat(opponent.recentForm().recentFive().winRate()).isEqualByComparingTo("0");
        assertThat(opponent.recentForm().recentFormSampleSize()).isEqualTo(5);
        assertThat(ours.recentForm().currentLosingStreak()).isEqualTo(1);
        assertThat(ours.recentForm().longestWinningStreak()).isEqualTo(5);
        assertThat(opponent.recentForm().currentLosingStreak()).isEqualTo(5);
        assertThat(input.comparison().recentFiveWinsDifference()).isEqualTo(4);
        assertThat(input.comparison().recentFormComparisonAvailable()).isTrue();
    }

    @Test
    void unavailableDataRetainsNullsAndPreventsComparison() {
        var legacy = mapper.toInput(metrics());
        assertThat(legacy.ourTeam().recentForm().recentFormAvailable()).isFalse();
        assertThat(legacy.ourTeam().recentForm().recentFive()).isNull();
        assertThat(legacy.ourTeam().recentForm().currentWinningStreak()).isNull();
        assertThat(legacy.ourTeam().recentForm().recentFormSampleSize()).isNull();
        assertThat(legacy.comparison().recentFormComparisonAvailable()).isFalse();
        assertThat(legacy.comparison().recentFiveWinsDifference()).isNull();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"SOURCE_UNAVAILABLE", "NO_COMPLETED_MATCHES", "MATCH_TIME_UNAVAILABLE"})
    void oneAvailableTeamDoesNotBecomeAnUnsupportedQuantitativeComparison(String status) {
        var json = new JacksonConfig().objectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode metrics = json.valueToTree(RecentFormAiFixture.metrics());
        ((com.fasterxml.jackson.databind.node.ObjectNode) metrics.at("/opponent/recentForm")).put("status", status);
        var input = mapper.toInput(json.convertValue(metrics, MatchOpponentMetricsResponse.class));
        assertThat(input.ourTeam().recentForm().recentFormAvailable()).isTrue();
        assertThat(input.opponent().recentForm().recentFormStatus()).isEqualTo(status);
        assertThat(input.opponent().recentForm().recentFive()).isNull();
        assertThat(input.opponent().recentForm().recentTenStatistics()).isNull();
        assertThat(input.comparison().recentFormComparisonAvailable()).isFalse();
    }

    @Test
    void insufficientMatchesRetainActualSampleAndWindows() {
        var json = new JacksonConfig().objectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode metrics = json.valueToTree(RecentFormAiFixture.metrics());
        var form = (com.fasterxml.jackson.databind.node.ObjectNode) metrics.at("/ourTeam/recentForm");
        form.put("status", "INSUFFICIENT_MATCHES").put("sampleSize", 2);
        var window = new com.yukai.team.matchservice.opponentanalysis.dto.RecentFormMetrics.Window(2, 1, 0, 1, 3, 2, new BigDecimal("0.5"));
        form.set("recentFive", json.valueToTree(window));
        form.set("recentTen", json.valueToTree(window));
        var team = (com.fasterxml.jackson.databind.node.ObjectNode) metrics.get("ourTeam");
        team.set("recentFive", json.valueToTree(List.of("lost", "won")));
        team.set("recentTen", json.valueToTree(List.of("lost", "won")));
        var input = mapper.toInput(json.convertValue(metrics, MatchOpponentMetricsResponse.class));
        assertThat(input.ourTeam().recentForm().recentFormAvailable()).isTrue();
        assertThat(input.ourTeam().recentForm().recentFormSampleSize()).isEqualTo(2);
        assertThat(input.ourTeam().recentForm().recentFive().sampleSize()).isEqualTo(2);
        assertThat(input.ourTeam().recentFive()).hasSize(2);
    }

    static MatchOpponentMetricsResponse metrics() { return RecentFormAiFixture.baseMetrics(); }

}
