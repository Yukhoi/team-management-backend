package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchResponse.CompetitionType;
import com.yukai.team.matchservice.opponentanalysis.dto.RecentFormMetrics;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

/** Pure computation of available completed league/cup matches across seasons and competitions. */
@Component
public class RecentFormCalculator {
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    public RecentFormMetrics calculate(Long teamId, List<FlaTeamMatchResponse> matches,
                                       OffsetDateTime cutoff, RecentFormMetrics.Source source) {
        if (teamId == null || teamId <= 0) {
            throw new IllegalArgumentException("flaTeamId must be positive");
        }
        if (cutoff == null) {
            return RecentFormMetrics.unavailable(RecentFormMetrics.Status.MATCH_TIME_UNAVAILABLE, source);
        }
        var targetDate = cutoff.atZoneSameInstant(PARIS).toLocalDate();
        var observedDate = source.fetchedAt().atZoneSameInstant(PARIS).toLocalDate();
        List<FlaTeamMatchResponse> eligible = matches.stream()
                .filter(match -> match.competitionType() == CompetitionType.LEAGUE
                        || match.competitionType() == CompetitionType.CUP)
                .filter(FlaTeamMatchResponse::hasCompletedScore)
                .filter(match -> match.resultFor(teamId) != null)
                // There is no final-whistle timestamp in FLA HTML. Exclude ALL same-day
                // games, even with kickoff times: kickoff ordering cannot prove completion.
                .filter(match -> match.matchDate() != null && match.matchDate().isBefore(targetDate))
                .filter(match -> !match.matchDate().isAfter(observedDate))
                .distinct()
                .sorted(Comparator.comparing(FlaTeamMatchResponse::matchDate).reversed()
                        .thenComparing(FlaTeamMatchResponse::kickoffTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<String> results = eligible.stream().map(match -> switch (match.resultFor(teamId)) {
            case WIN -> "won";
            case DRAW -> "draw";
            case LOSS -> "lost";
        }).toList();
        RecentFormMetrics.Status status = eligible.isEmpty() ? RecentFormMetrics.Status.NO_COMPLETED_MATCHES
                : eligible.size() < 5 ? RecentFormMetrics.Status.INSUFFICIENT_MATCHES : RecentFormMetrics.Status.AVAILABLE;
        return new RecentFormMetrics(status, eligible.size(), results,
                window(teamId, eligible.stream().limit(5).toList()),
                window(teamId, eligible.stream().limit(10).toList()), source,
                RecentFormMetrics.CompetitionScope.ALL_COMPETITIONS, RecentFormMetrics.SeasonScope.CROSS_SEASON_ALLOWED);
    }

    private RecentFormMetrics.Window window(Long teamId, List<FlaTeamMatchResponse> matches) {
        int wins = 0, draws = 0, losses = 0, goalsFor = 0, goalsAgainst = 0;
        for (FlaTeamMatchResponse match : matches) {
            switch (match.resultFor(teamId)) {
                case WIN -> wins++;
                case DRAW -> draws++;
                case LOSS -> losses++;
            }
            boolean home = teamId.equals(match.homeTeamId());
            goalsFor += home ? match.homeScore() : match.awayScore();
            goalsAgainst += home ? match.awayScore() : match.homeScore();
        }
        BigDecimal winRate = matches.isEmpty() ? null
                : BigDecimal.valueOf(wins).divide(BigDecimal.valueOf(matches.size()), 2, RoundingMode.HALF_UP);
        return new RecentFormMetrics.Window(matches.size(), wins, draws, losses, goalsFor, goalsAgainst, winRate);
    }
}
