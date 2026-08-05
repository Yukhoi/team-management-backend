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
        assertThat(input.ourTeam().recentFive()).containsExactly("won", "lost", "draw", "won", "lost");
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
        assertThat(json).doesNotContain("rawPayload", "payloadHash", "championnatId", "saisonId");
    }

    private MatchOpponentMetricsResponse metrics() {
        return new MatchOpponentMetricsResponse(
                new MatchOpponentMetricsResponse.MatchContext(42L, 5L, "Paris League", "2026", 12L, "YEXIAO PARIS FC", 13L, "ASTERIA", OffsetDateTime.parse("2026-08-10T18:30:00Z"), HomeAway.HOME, MatchStatus.SCHEDULED),
                team(12L, "YEXIAO PARIS FC", 12, 20, 6, 3, 17, 63, 134, "2.42", "5.15", "0.23", List.of("won", "lost", "draw", "won", "lost")),
                team(13L, "ASTERIA", 3, 68, 22, 2, 2, 112, 40, "4.31", "1.54", "0.85", List.of("won", "won", "won", "draw", "won")),
                new LeaguePerformanceMetrics(14, null, new BigDecimal("35.10"), new BigDecimal("1.35"), null, null, new BigDecimal("2.58"), new BigDecimal("2.58"), 112, 40, 68, new BigDecimal("0.42"), BigDecimal.ZERO),
                new TeamComparisonMetrics(-48, 9, new BigDecimal("-0.62"), new BigDecimal("-1.20"), new BigDecimal("-1.89"), new BigDecimal("3.61"), -143, -2, new BigDecimal("-0.84"), new BigDecimal("0.88")),
                new MatchOpponentMetricsResponse.DataSource("FLA", 1365L, 15L, 10L, "hash", OffsetDateTime.parse("2026-08-05T12:00:00Z"), CacheStatus.HIT, 21600L, false, "LATEST_TO_OLDEST", "CALCULATED")
        );
    }

    private TeamPerformanceMetrics team(Long id, String name, int rank, int points, int wins, int draws, int losses, int gf, int ga, String gfpg, String gapg, String winRate, List<String> recentFive) {
        return new TeamPerformanceMetrics(id, id + 1000, name, rank, points, wins + draws + losses, wins, draws, losses, new BigDecimal(winRate), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, gf, ga, gf - ga, new BigDecimal(gfpg), new BigDecimal(gapg), 0, 0, recentFive, recentFive, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
