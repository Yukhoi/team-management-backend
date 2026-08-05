package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpponentMetricsCalculatorTest {

    private final OpponentMetricsCalculator calculator = new OpponentMetricsCalculator();

    @Test
    void calculatesTeamMetricsAndRecentForm() {
        FlaStandingEntryResponse entry = entry(6580L, "A Team", 21, 6, 3, 1, 20, 10);
        entry.setBonus(-1);
        entry.setForfeits(0);
        entry.setForm(List.of("won", "won", "draw", "lost", "won", "lost", "lost", "won", "draw", "bad", "won"));

        var metrics = calculator.calculateTeamMetrics(12L, 6580L, entry, 1);

        assertThat(metrics.played()).isEqualTo(10);
        assertThat(metrics.winRate()).isEqualByComparingTo(new BigDecimal("0.60"));
        assertThat(metrics.drawRate()).isEqualByComparingTo(new BigDecimal("0.30"));
        assertThat(metrics.lossRate()).isEqualByComparingTo(new BigDecimal("0.10"));
        assertThat(metrics.pointsPerGame()).isEqualByComparingTo(new BigDecimal("2.10"));
        assertThat(metrics.goalsForPerGame()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(metrics.goalsAgainstPerGame()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(metrics.goalDifference()).isEqualTo(10);
        assertThat(metrics.recentFive()).containsExactly("won", "won", "draw", "lost", "won");
        assertThat(metrics.recentTen()).containsExactly("won", "won", "draw", "lost", "won", "lost", "lost", "won", "draw", "UNKNOWN");
        assertThat(metrics.currentWinningStreak()).isEqualTo(2);
        assertThat(metrics.currentDrawingStreak()).isZero();
        assertThat(metrics.currentLosingStreak()).isZero();
        assertThat(metrics.currentUnbeatenStreak()).isEqualTo(3);
        assertThat(metrics.longestWinningStreak()).isEqualTo(2);
        assertThat(metrics.longestLosingStreak()).isEqualTo(2);
        assertThat(metrics.recentFiveWins()).isEqualTo(3);
        assertThat(metrics.recentFiveDraws()).isEqualTo(1);
        assertThat(metrics.recentFiveLosses()).isEqualTo(1);
        assertThat(metrics.recentTenWins()).isEqualTo(4);
        assertThat(metrics.recentTenDraws()).isEqualTo(2);
        assertThat(metrics.recentTenLosses()).isEqualTo(3);
    }

    @Test
    void handlesNullFormEmptyFormAndZeroPlayedWithoutDivisionByZero() {
        FlaStandingEntryResponse nullForm = entry(6580L, "A Team", 0, 0, 0, 0, 0, 0);
        nullForm.setForm(null);
        var nullFormMetrics = calculator.calculateTeamMetrics(12L, 6580L, nullForm, 1);

        assertThat(nullFormMetrics.played()).isZero();
        assertThat(nullFormMetrics.winRate()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(nullFormMetrics.pointsPerGame()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(nullFormMetrics.recentFive()).isEmpty();
        assertThat(nullFormMetrics.currentUnbeatenStreak()).isZero();

        FlaStandingEntryResponse emptyForm = entry(6581L, "B Team", null, null, null, null, null, null);
        emptyForm.setForm(List.of());
        var emptyFormMetrics = calculator.calculateTeamMetrics(13L, 6581L, emptyForm, 2);

        assertThat(emptyFormMetrics.points()).isZero();
        assertThat(emptyFormMetrics.goalsFor()).isZero();
        assertThat(emptyFormMetrics.recentTen()).isEmpty();
    }

    @Test
    void calculatesRanksWithStableTieBreakers() {
        FlaStandingEntryResponse first = entry(1L, "Alpha", 20, 6, 2, 2, 18, 8);
        FlaStandingEntryResponse second = entry(2L, "Bravo", 20, 6, 2, 2, 17, 7);
        FlaStandingEntryResponse third = entry(3L, "Charlie", 20, 6, 2, 2, 14, 9);
        FlaStandingEntryResponse fourth = entry(4L, "Delta", 19, 6, 1, 3, 30, 10);

        var ranks = calculator.calculateRanks(List.of(fourth, third, second, first));

        assertThat(ranks.get(1L)).isEqualTo(1);
        assertThat(ranks.get(2L)).isEqualTo(2);
        assertThat(ranks.get(3L)).isEqualTo(3);
        assertThat(ranks.get(4L)).isEqualTo(4);
    }

    @Test
    void calculatesLeagueAverages() {
        FlaStandingEntryResponse first = entry(1L, "A Team", 20, 6, 2, 2, 30, 10);
        FlaStandingEntryResponse second = entry(2L, "B Team", 10, 3, 1, 6, 10, 20);

        var league = calculator.calculateLeagueMetrics(List.of(first, second));

        assertThat(league.teamCount()).isEqualTo(2);
        assertThat(league.totalMatchesApproximation()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(league.averagePoints()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(league.averagePointsPerGame()).isEqualByComparingTo(new BigDecimal("1.50"));
        assertThat(league.averageGoalsFor()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(league.averageGoalsAgainst()).isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(league.averageGoalsForPerGame()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(league.averageGoalsAgainstPerGame()).isEqualByComparingTo(new BigDecimal("1.50"));
        assertThat(league.highestGoalsFor()).isEqualTo(30);
        assertThat(league.lowestGoalsAgainst()).isEqualTo(10);
        assertThat(league.highestPoints()).isEqualTo(20);
        assertThat(league.averageWinRate()).isEqualByComparingTo(new BigDecimal("0.45"));
        assertThat(league.averageGoalDifference()).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void calculatesComparisonDifferences() {
        var our = calculator.calculateTeamMetrics(12L, 6580L, entry(6580L, "A Team", 20, 6, 2, 2, 30, 10), 1);
        var opponent = calculator.calculateTeamMetrics(13L, 6581L, entry(6581L, "B Team", 10, 3, 1, 6, 10, 20), 3);

        var comparison = calculator.compare(our, opponent);

        assertThat(comparison.pointsDifference()).isEqualTo(10);
        assertThat(comparison.rankDifference()).isEqualTo(-2);
        assertThat(comparison.winRateDifference()).isEqualByComparingTo(new BigDecimal("0.30"));
        assertThat(comparison.pointsPerGameDifference()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(comparison.goalsForPerGameDifference()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(comparison.goalsAgainstPerGameDifference()).isEqualByComparingTo(new BigDecimal("-1.00"));
        assertThat(comparison.goalDifferenceDifference()).isEqualTo(30);
        assertThat(comparison.opponentAttackVsOurDefenseGap()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(comparison.ourAttackVsOpponentDefenseGap()).isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    void returnsZeroLeagueMetricsForNullStandings() {
        var league = calculator.calculateLeagueMetrics(null);

        assertThat(league.teamCount()).isZero();
        assertThat(league.averagePoints()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(league.highestPoints()).isZero();
    }

    private FlaStandingEntryResponse entry(
            Long teamId,
            String teamName,
            Integer points,
            Integer wins,
            Integer draws,
            Integer losses,
            Integer goalsFor,
            Integer goalsAgainst
    ) {
        FlaStandingEntryResponse entry = new FlaStandingEntryResponse();
        entry.setTeamId(teamId);
        entry.setTeamName(teamName);
        entry.setChampionnatId(1365L);
        entry.setPoints(points);
        entry.setWins(wins);
        entry.setDraws(draws);
        entry.setLosses(losses);
        entry.setGoalsFor(goalsFor);
        entry.setGoalsAgainst(goalsAgainst);
        entry.setBonus(0);
        entry.setForfeits(0);
        entry.setForm(List.of("won", "lost"));
        return entry;
    }
}
