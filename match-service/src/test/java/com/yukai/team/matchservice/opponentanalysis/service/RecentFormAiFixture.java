package com.yukai.team.matchservice.opponentanalysis.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yukai.team.matchservice.config.JacksonConfig;
import com.yukai.team.matchservice.opponentanalysis.dto.MatchOpponentMetricsResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamPerformanceMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamComparisonMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.LeaguePerformanceMetrics;
import com.yukai.team.matchservice.entity.HomeAway;
import com.yukai.team.matchservice.entity.MatchStatus;
import java.time.OffsetDateTime;
import com.yukai.team.matchservice.opponentanalysis.dto.RecentFormMetrics;
import java.math.BigDecimal;
import java.util.List;

/** Reported match 11 statistics; remaining standings values are synthetic test data. */
public final class RecentFormAiFixture {
    public static MatchOpponentMetricsResponse metrics() {
        var mapper = new JacksonConfig().objectMapper();
        ObjectNode root = mapper.valueToTree(baseMetrics());
        ((ObjectNode) root.get("match")).put("matchId", 11).put("ourTeamName", "Coach Docker Team")
                .put("opponentTeamName", "FLA Test Team 1");
        setTeam((ObjectNode) root.get("ourTeam"), 270, "YEXIAO PARIS FC", true);
        setTeam((ObjectNode) root.get("opponent"), 204, "PARIS CROCOS FC", false);
        ((ObjectNode) root.get("comparison")).put("recentFiveWinsDifference", 4);
        return mapper.convertValue(root, MatchOpponentMetricsResponse.class);
    }

    private static void setTeam(ObjectNode team, long flaId, String name, boolean ours) {
        var mapper = new JacksonConfig().objectMapper();
        var five = ours ? List.of("lost", "won", "won", "won", "won")
                : List.of("lost", "lost", "lost", "lost", "lost");
        var ten = ours ? List.of("lost", "won", "won", "won", "won", "won") : five;
        team.put("flaTeamId", flaId).put("teamName", name);
        team.set("recentFive", mapper.valueToTree(five));
        team.set("recentTen", mapper.valueToTree(ten));
        team.put("currentWinningStreak", 0).put("currentDrawingStreak", 0)
                .put("currentLosingStreak", ours ? 1 : 5).put("currentUnbeatenStreak", 0)
                .put("longestWinningStreak", ours ? 5 : 0).put("longestLosingStreak", ours ? 1 : 5);
        var window = new RecentFormMetrics.Window(5, ours ? 4 : 0, 0, ours ? 1 : 5,
                ours ? 27 : 6, ours ? 8 : 16, ours ? new BigDecimal("0.80") : BigDecimal.ZERO);
        // Sixth match score is synthetic; reported aggregate is only known for the latest five.
        var tenWindow = ours ? new RecentFormMetrics.Window(6, 5, 0, 1, 30, 9, new BigDecimal("0.8333")) : window;
        var source = new RecentFormMetrics.Source(flaId, 15L, "recent-" + flaId, "recent-" + flaId,
                null, CacheStatus.HIT, 21600, false, null);
        team.set("recentForm", mapper.valueToTree(new RecentFormMetrics(RecentFormMetrics.Status.AVAILABLE,
                ours ? 6 : 5, ten, window, tenWindow, source,
                RecentFormMetrics.CompetitionScope.ALL_COMPETITIONS, RecentFormMetrics.SeasonScope.CROSS_SEASON_ALLOWED)));
    }
    static MatchOpponentMetricsResponse baseMetrics() {
        return new MatchOpponentMetricsResponse(
                new MatchOpponentMetricsResponse.MatchContext(42L, 5L, "Paris League", "2026", 12L, "YEXIAO PARIS FC", 13L, "ASTERIA", OffsetDateTime.parse("2026-08-10T18:30:00Z"), HomeAway.HOME, MatchStatus.SCHEDULED),
                team(12L, "YEXIAO PARIS FC", 12, 20, 6, 3, 17, 63, 134, "2.42", "5.15", "0.23", List.of("won", "lost", "draw", "won", "lost")),
                team(13L, "ASTERIA", 3, 68, 22, 2, 2, 112, 40, "4.31", "1.54", "0.85", List.of("won", "won", "won", "draw", "won")),
                new LeaguePerformanceMetrics(14, null, new BigDecimal("35.10"), new BigDecimal("1.35"), null, null, new BigDecimal("2.58"), new BigDecimal("2.58"), 112, 40, 68, new BigDecimal("0.42"), BigDecimal.ZERO),
                new TeamComparisonMetrics(-48, 9, new BigDecimal("-0.62"), new BigDecimal("-1.20"), new BigDecimal("-1.89"), new BigDecimal("3.61"), -143, -2, new BigDecimal("-0.84"), new BigDecimal("0.88")),
                new MatchOpponentMetricsResponse.DataSource("FLA", 1365L, 15L, 10L, "hash", OffsetDateTime.parse("2026-08-05T12:00:00Z"), CacheStatus.HIT, 21600L, false, "LATEST_TO_OLDEST", "CALCULATED")
        );
    }

    private static TeamPerformanceMetrics team(Long id, String name, int rank, int points, int wins, int draws, int losses, int gf, int ga, String gfpg, String gapg, String winRate, List<String> recentFive) {
        return new TeamPerformanceMetrics(id, id + 1000, name, rank, points, wins + draws + losses, wins, draws, losses, new BigDecimal(winRate), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE, gf, ga, gf - ga, new BigDecimal(gfpg), new BigDecimal(gapg), 0, 0, recentFive, recentFive, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
