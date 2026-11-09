package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchResponse.CompetitionType;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchResponse.MatchStatus;
import com.yukai.team.matchservice.opponentanalysis.dto.RecentFormMetrics;
import com.yukai.team.matchservice.opponentanalysis.parser.FlaTeamMatchesHtmlParser;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecentFormCalculatorTest {
    static final String LEAGUE = "Foot à 7 - Vendredi - 2ème division";
    static final OffsetDateTime TARGET = OffsetDateTime.parse("2026-11-13T20:00:00Z");
    static final OffsetDateTime FETCHED = OffsetDateTime.parse("2026-11-09T17:00:00Z");
    private final RecentFormCalculator calculator = new RecentFormCalculator();
    private final OpponentMetricsCalculator metrics = new OpponentMetricsCalculator();

    @Test
    void fixturesUseOfficialCompetitionsAndExistingResultEncoding() throws IOException {
        var ours = calculate(270, fixture(270), TARGET);
        var opponent = calculate(63, fixture(63), TARGET);
        var a = team(270, ours);
        var b = team(63, opponent);
        assertThat(a.recentFive()).containsExactly("lost", "won", "won", "won", "won");
        assertThat(a.recentTen()).containsExactly("lost", "won", "won", "won", "won", "won");
        assertThat(a.recentFiveWins()).isEqualTo(4);
        assertThat(a.recentTenWins()).isEqualTo(5);
        assertThat(a.recentFiveDraws()).isZero();
        assertThat(a.recentFiveLosses()).isEqualTo(1);
        assertThat(a.currentLosingStreak()).isEqualTo(1);
        assertThat(a.currentWinningStreak()).isZero();
        assertThat(a.currentUnbeatenStreak()).isZero();
        assertThat(a.longestWinningStreak()).isEqualTo(5);
        assertThat(a.longestLosingStreak()).isEqualTo(1);
        assertThat(ours.recentFive()).isEqualTo(new RecentFormMetrics.Window(5, 4, 0, 1, 27, 8, new java.math.BigDecimal("0.80")));
        assertThat(ours.recentTen()).isEqualTo(new RecentFormMetrics.Window(6, 5, 0, 1, 31, 9, new java.math.BigDecimal("0.83")));
        assertThat(b.recentFive()).containsExactly("won", "won", "draw", "lost", "lost");
        assertThat(b.recentTen()).containsExactly("won", "won", "draw", "lost", "lost", "draw", "draw");
        assertThat(opponent.sampleSize()).isEqualTo(7);
        assertThat(opponent.status()).isEqualTo(RecentFormMetrics.Status.AVAILABLE);
        assertThat(ours.status()).isEqualTo(RecentFormMetrics.Status.AVAILABLE);
        assertThat(opponent.recentFive().goalsFor()).isEqualTo(13);
        assertThat(opponent.recentFive().goalsAgainst()).isEqualTo(14);
        assertThat(opponent.recentFive().winRate()).isEqualByComparingTo("0.40");
        assertThat(b.currentWinningStreak()).isEqualTo(2);
        assertThat(b.currentUnbeatenStreak()).isEqualTo(3);
        assertThat(metrics.compare(a, b).recentFiveWinsDifference()).isEqualTo(2);
        assertThat(metrics.compare(a, b).recentFormComparisonAvailable()).isTrue();
    }

    @Test
    void pastTargetCannotConsumeNovemberResults() throws IOException {
        var result = calculate(270, fixture(270), OffsetDateTime.parse("2026-08-01T00:00:00Z"));
        assertThat(result.status()).isEqualTo(RecentFormMetrics.Status.NO_COMPLETED_MATCHES);
        assertThat(result.sampleSize()).isZero();
        assertThat(result.recentFive().winRate()).isNull();
        assertThat(team(270, result).recentFiveWins()).isNull();
        assertThat(team(270, result).currentWinningStreak()).isNull();
    }

    @Test
    void excludesSameParisDayRegardlessOfKickoffAndAccountsForUtcBoundary() {
        var records = List.of(game("2026-11-06", 3, 0), game("2026-11-07", 4, 0));
        // 23:30 UTC is already Nov 7 in Paris. Nov 7 must be excluded, Nov 6 allowed.
        var result = calculate(270, records, OffsetDateTime.parse("2026-11-06T23:30:00Z"));
        assertThat(result.sampleSize()).isEqualTo(1);
        assertThat(result.recentFive().goalsFor()).isEqualTo(3);
        var sameDayEarlyKickoff = new FlaTeamMatchResponse(LocalDate.of(2026, 11, 7), LocalTime.of(1, 0),
                270L, "Ours", null, "Them", 1, 0, CompetitionType.LEAGUE, LEAGUE, null, MatchStatus.COMPLETED);
        assertThat(calculate(270, List.of(sameDayEarlyKickoff), OffsetDateTime.parse("2026-11-07T21:00:00Z")).sampleSize()).isZero();
    }

    @Test
    void officialCompetitionsAcrossLeaguesAreIncludedBeforeWindowTruncation() {
        List<FlaTeamMatchResponse> records = new ArrayList<>();
        for (int i = 1; i <= 12; i++) records.add(game("2026-10-" + String.format("%02d", i), 1, 0));
        records.add(new FlaTeamMatchResponse(LocalDate.of(2026, 11, 8), null, 270L, "Ours", null, "Them",
                99, 0, CompetitionType.CUP, LEAGUE, null, MatchStatus.COMPLETED));
        records.add(new FlaTeamMatchResponse(LocalDate.of(2026, 11, 7), null, 270L, "Ours", null, "Them",
                99, 0, CompetitionType.LEAGUE, LEAGUE + " B", null, MatchStatus.COMPLETED));
        records.add(game("2027-01-01", 99, 0));
        records.add(records.get(0));
        var result = calculate(270, records, TARGET);
        assertThat(result.status()).isEqualTo(RecentFormMetrics.Status.AVAILABLE);
        assertThat(result.sampleSize()).isEqualTo(14);
        assertThat(result.recentFive().sampleSize()).isEqualTo(5);
        assertThat(result.recentTen().sampleSize()).isEqualTo(10);
        assertThat(result.recentTen().goalsFor()).isEqualTo(206);
        assertThat(team(270, result).longestWinningStreak()).isEqualTo(14);
    }

    @Test
    void drawsZeroScoresAndStreaksAreComputedFromMostRecentMatch() {
        var result = calculate(270, List.of(game("2026-11-01", 0, 0), game("2026-10-31", 1, 1),
                game("2026-10-30", 2, 0), game("2026-10-29", 0, 1), game("2026-10-28", 0, 2)), TARGET);
        var team = team(270, result);
        assertThat(team.currentDrawingStreak()).isEqualTo(2);
        assertThat(team.currentUnbeatenStreak()).isEqualTo(3);
        assertThat(team.longestLosingStreak()).isEqualTo(2);
        assertThat(result.recentFive().draws()).isEqualTo(2);
        assertThat(result.recentFive().goalsFor()).isEqualTo(3);
        assertThat(result.recentFive().goalsAgainst()).isEqualTo(4);
        assertThat(result.recentFive().winRate()).isEqualByComparingTo("0.20");
    }

    @Test
    void threeMatchWinRateUsesThreeAsDenominatorAndRealLossHasZeroWins() {
        var result = calculate(270, List.of(game("2026-11-01", 2, 0), game("2026-10-31", 1, 0), game("2026-10-30", 0, 1)), TARGET);
        assertThat(result.status()).isEqualTo(RecentFormMetrics.Status.INSUFFICIENT_MATCHES);
        assertThat(result.available()).isTrue();
        assertThat(result.recentFive().sampleSize()).isEqualTo(3);
        assertThat(result.recentFive().winRate()).isEqualByComparingTo("0.67");
        var losses = team(270, calculate(270, List.of(game("2026-11-01", 0, 1), game("2026-10-31", 0, 3)), TARGET));
        assertThat(losses.recentFiveWins()).isZero();
        assertThat(losses.currentLosingStreak()).isEqualTo(2);
        assertThat(losses.recentFormAvailable()).isTrue();
    }

    @Test
    void missingSourceNeverFallsBackToStandingsFormOrMisleadingComparisonZero() throws IOException {
        var missing = team(63, RecentFormMetrics.unavailable(RecentFormMetrics.Status.SOURCE_UNAVAILABLE, null));
        assertThat(missing.recentFive()).isEmpty();
        assertThat(missing.recentFiveWins()).isNull();
        assertThat(missing.longestLosingStreak()).isNull();
        assertThat(missing.recentFormSampleSize()).isNull();
        var comparison = metrics.compare(team(270, calculate(270, fixture(270), TARGET)), missing);
        assertThat(comparison.recentFiveWinsDifference()).isNull();
        assertThat(comparison.recentFormComparisonAvailable()).isFalse();
        var realZero = metrics.compare(missingWithKnownZeroWins(), missingWithKnownZeroWins());
        assertThat(realZero.recentFiveWinsDifference()).isZero();
        assertThat(realZero.recentFormComparisonAvailable()).isTrue();
    }

    @Test
    void missingTargetIsUnavailableAndFutureCompletedRecordBeyondObservationIsRejected() {
        assertThat(calculate(270, List.of(), null).status()).isEqualTo(RecentFormMetrics.Status.MATCH_TIME_UNAVAILABLE);
        assertThat(calculate(270, List.of(game("2026-11-10", 1, 0)), TARGET).sampleSize()).isZero();
    }

    @Test
    void unknownCompetitionUnfinishedAndInvalidScoresAreNotFormalCompletedMatches() {
        List<FlaTeamMatchResponse> records = new ArrayList<>();
        records.add(game("2026-11-01", 1, 0));
        for (CompetitionType type : new CompetitionType[]{CompetitionType.OTHER, null}) {
            records.add(new FlaTeamMatchResponse(LocalDate.of(2026, 11, 2), null, 270L, "Ours", null, "Them",
                    99, 0, type, "Unverified type", null, MatchStatus.COMPLETED));
        }
        records.add(new FlaTeamMatchResponse(LocalDate.of(2026, 11, 3), null, 270L, "Ours", null, "Them",
                99, 0, CompetitionType.CUP, "Cup", null, MatchStatus.UNKNOWN));
        records.add(new FlaTeamMatchResponse(LocalDate.of(2026, 11, 4), null, 270L, "Ours", null, "Them",
                null, 0, CompetitionType.CUP, "Cup", null, MatchStatus.COMPLETED));
        records.add(new FlaTeamMatchResponse(LocalDate.of(2026, 11, 5), null, 270L, "Ours", null, "Them",
                -1, 0, CompetitionType.LEAGUE, LEAGUE, null, MatchStatus.COMPLETED));
        var result = calculate(270, records, TARGET);
        assertThat(result.results()).containsExactly("won");
        assertThat(result.sampleSize()).isEqualTo(1);
        assertThat(result.recentFive().goalsFor()).isEqualTo(1);
    }

    @Test
    void sameDateSortsByKickoffDescendingWithUnknownTimesLastAndAllowsPriorSeasons() {
        var date = LocalDate.of(2026, 11, 5);
        var result = calculate(270, List.of(
                new FlaTeamMatchResponse(date, null, 270L, "Ours", null, "Them", 0, 0,
                        CompetitionType.CUP, "Cup", null, MatchStatus.COMPLETED),
                new FlaTeamMatchResponse(date, LocalTime.of(19, 0), 270L, "Ours", null, "Them", 1, 0,
                        CompetitionType.LEAGUE, "Another league", null, MatchStatus.COMPLETED),
                new FlaTeamMatchResponse(date, LocalTime.of(21, 0), 270L, "Ours", null, "Them", 0, 1,
                        CompetitionType.CUP, "Another cup", null, MatchStatus.COMPLETED),
                game("2025-11-01", 1, 0)), TARGET);
        assertThat(result.results()).containsExactly("lost", "won", "draw", "won");
        assertThat(result.status()).isEqualTo(RecentFormMetrics.Status.INSUFFICIENT_MATCHES);
        assertThat(result.sampleSize()).isEqualTo(4);
        assertThat(result.competitionScope()).isEqualTo(RecentFormMetrics.CompetitionScope.ALL_COMPETITIONS);
        assertThat(result.seasonScope()).isEqualTo(RecentFormMetrics.SeasonScope.CROSS_SEASON_ALLOWED);
    }

    private com.yukai.team.matchservice.opponentanalysis.dto.TeamPerformanceMetrics missingWithKnownZeroWins() {
        return team(270, calculate(270, List.of(game("2026-11-01", 0, 0)), TARGET));
    }

    private RecentFormMetrics calculate(long id, List<FlaTeamMatchResponse> records, OffsetDateTime target) {
        return calculator.calculate(id, records, target, new RecentFormMetrics.Source(id, null, "hash", "hash",
                FETCHED, CacheStatus.MISS, 21600, false, null));
    }

    private com.yukai.team.matchservice.opponentanalysis.dto.TeamPerformanceMetrics team(long id, RecentFormMetrics form) {
        FlaStandingEntryResponse entry = new FlaStandingEntryResponse();
        entry.setTeamName("Team " + id);
        entry.setForm(List.of("won", "won", "won")); // Must not be used by the integrated path.
        return metrics.calculateTeamMetrics(id, id, entry, 1, form);
    }

    static List<FlaTeamMatchResponse> fixture(long id) throws IOException {
        return new FlaTeamMatchesHtmlParser().parse(new ClassPathResource("fla/team-" + id + ".html")
                .getContentAsString(StandardCharsets.UTF_8), id);
    }

    private FlaTeamMatchResponse game(String date, int own, int opponent) {
        return new FlaTeamMatchResponse(LocalDate.parse(date), null, 270L, "Ours", null, "Them", own, opponent,
                CompetitionType.LEAGUE, LEAGUE, null, MatchStatus.COMPLETED);
    }
}
