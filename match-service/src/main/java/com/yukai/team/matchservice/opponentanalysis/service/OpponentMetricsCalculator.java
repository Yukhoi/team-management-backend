package com.yukai.team.matchservice.opponentanalysis.service;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.LeaguePerformanceMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamComparisonMetrics;
import com.yukai.team.matchservice.opponentanalysis.dto.TeamPerformanceMetrics;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class OpponentMetricsCalculator {

    private static final String WON = "won";
    private static final String DRAW = "draw";
    private static final String LOST = "lost";
    private static final String UNKNOWN = "UNKNOWN";

    public TeamPerformanceMetrics calculateTeamMetrics(
            Long internalTeamId,
            Long flaTeamId,
            FlaStandingEntryResponse entry,
            Integer calculatedRank
    ) {
        if (entry == null) {
            entry = new FlaStandingEntryResponse();
            entry.setTeamId(flaTeamId);
        }
        Long resolvedFlaTeamId = flaTeamId == null ? entry.getTeamId() : flaTeamId;
        int wins = value(entry.getWins());
        int draws = value(entry.getDraws());
        int losses = value(entry.getLosses());
        int played = wins + draws + losses;
        int goalsFor = value(entry.getGoalsFor());
        int goalsAgainst = value(entry.getGoalsAgainst());
        int goalDifference = goalsFor - goalsAgainst;
        List<String> form = normalizeForm(entry.getForm());
        List<String> recentFive = form.stream().limit(5).toList();
        List<String> recentTen = form.stream().limit(10).toList();

        return new TeamPerformanceMetrics(
                internalTeamId,
                resolvedFlaTeamId,
                entry.getTeamName(),
                calculatedRank,
                value(entry.getPoints()),
                played,
                wins,
                draws,
                losses,
                divide(wins, played),
                divide(draws, played),
                divide(losses, played),
                divide(value(entry.getPoints()), played),
                goalsFor,
                goalsAgainst,
                goalDifference,
                divide(goalsFor, played),
                divide(goalsAgainst, played),
                value(entry.getBonus()),
                value(entry.getForfeits()),
                recentFive,
                recentTen,
                currentStreak(form, WON),
                currentStreak(form, DRAW),
                currentStreak(form, LOST),
                currentUnbeatenStreak(form),
                longestStreak(form, WON),
                longestStreak(form, LOST),
                count(recentFive, WON),
                count(recentFive, DRAW),
                count(recentFive, LOST),
                count(recentTen, WON),
                count(recentTen, DRAW),
                count(recentTen, LOST)
        );
    }

    public LeaguePerformanceMetrics calculateLeagueMetrics(List<FlaStandingEntryResponse> standings) {
        List<TeamPerformanceMetrics> metrics = safeStandings(standings).stream()
                .map(entry -> calculateTeamMetrics(null, entry.getTeamId(), entry, null))
                .toList();
        int teamCount = metrics.size();
        int totalPlayed = metrics.stream().mapToInt(TeamPerformanceMetrics::played).sum();
        return new LeaguePerformanceMetrics(
                teamCount,
                divide(totalPlayed, 2),
                averageInt(metrics.stream().map(TeamPerformanceMetrics::points).toList()),
                averageDecimal(metrics.stream().map(TeamPerformanceMetrics::pointsPerGame).toList()),
                averageInt(metrics.stream().map(TeamPerformanceMetrics::goalsFor).toList()),
                averageInt(metrics.stream().map(TeamPerformanceMetrics::goalsAgainst).toList()),
                averageDecimal(metrics.stream().map(TeamPerformanceMetrics::goalsForPerGame).toList()),
                averageDecimal(metrics.stream().map(TeamPerformanceMetrics::goalsAgainstPerGame).toList()),
                metrics.stream().mapToInt(TeamPerformanceMetrics::goalsFor).max().orElse(0),
                metrics.stream().mapToInt(TeamPerformanceMetrics::goalsAgainst).min().orElse(0),
                metrics.stream().mapToInt(TeamPerformanceMetrics::points).max().orElse(0),
                averageDecimal(metrics.stream().map(TeamPerformanceMetrics::winRate).toList()),
                averageInt(metrics.stream().map(TeamPerformanceMetrics::goalDifference).toList())
        );
    }

    public TeamComparisonMetrics compare(TeamPerformanceMetrics ourTeam, TeamPerformanceMetrics opponent) {
        return new TeamComparisonMetrics(
                ourTeam.points() - opponent.points(),
                ourTeam.calculatedRank() - opponent.calculatedRank(),
                ourTeam.winRate().subtract(opponent.winRate()).setScale(2, RoundingMode.HALF_UP),
                ourTeam.pointsPerGame().subtract(opponent.pointsPerGame()).setScale(2, RoundingMode.HALF_UP),
                ourTeam.goalsForPerGame().subtract(opponent.goalsForPerGame()).setScale(2, RoundingMode.HALF_UP),
                ourTeam.goalsAgainstPerGame().subtract(opponent.goalsAgainstPerGame()).setScale(2, RoundingMode.HALF_UP),
                ourTeam.goalDifference() - opponent.goalDifference(),
                ourTeam.recentFiveWins() - opponent.recentFiveWins(),
                opponent.goalsForPerGame().subtract(ourTeam.goalsAgainstPerGame()).setScale(2, RoundingMode.HALF_UP),
                ourTeam.goalsForPerGame().subtract(opponent.goalsAgainstPerGame()).setScale(2, RoundingMode.HALF_UP)
        );
    }

    public Map<Long, Integer> calculateRanks(List<FlaStandingEntryResponse> standings) {
        List<FlaStandingEntryResponse> sorted = safeStandings(standings).stream()
                .sorted(Comparator
                        .comparingInt((FlaStandingEntryResponse entry) -> value(entry.getPoints())).reversed()
                        .thenComparing(Comparator.comparingInt(this::goalDifference).reversed())
                        .thenComparing(Comparator.comparingInt((FlaStandingEntryResponse entry) -> value(entry.getGoalsFor())).reversed())
                        .thenComparing(entry -> entry.getTeamName() == null ? "" : entry.getTeamName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(entry -> entry.getTeamId() == null ? Long.MAX_VALUE : entry.getTeamId()))
                .toList();
        Map<Long, Integer> ranks = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getTeamId() != null) {
                ranks.put(sorted.get(i).getTeamId(), i + 1);
            }
        }
        return ranks;
    }

    private List<FlaStandingEntryResponse> safeStandings(List<FlaStandingEntryResponse> standings) {
        if (standings == null) {
            return List.of();
        }
        return standings.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private int goalDifference(FlaStandingEntryResponse entry) {
        return value(entry.getGoalsFor()) - value(entry.getGoalsAgainst());
    }

    private BigDecimal averageInt(List<Integer> values) {
        if (values.isEmpty()) {
            return decimal(0);
        }
        int sum = values.stream().mapToInt(Integer::intValue).sum();
        return divide(sum, values.size());
    }

    private BigDecimal averageDecimal(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return decimal(0);
        }
        BigDecimal sum = values.stream().reduce(decimal(0), BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(values.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal divide(int numerator, int denominator) {
        if (denominator == 0) {
            return decimal(0);
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(int value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private List<String> normalizeForm(List<String> form) {
        if (form == null) {
            return List.of();
        }
        return form.stream()
                .map(this::normalizeResult)
                .toList();
    }

    private String normalizeResult(String result) {
        if (result == null) {
            return UNKNOWN;
        }
        String normalized = result.trim().toLowerCase(Locale.ROOT);
        if (WON.equals(normalized) || DRAW.equals(normalized) || LOST.equals(normalized)) {
            return normalized;
        }
        return UNKNOWN;
    }

    private int currentStreak(List<String> form, String expected) {
        int streak = 0;
        for (String result : form) {
            if (!expected.equals(result)) {
                break;
            }
            streak++;
        }
        return streak;
    }

    private int currentUnbeatenStreak(List<String> form) {
        int streak = 0;
        for (String result : form) {
            if (LOST.equals(result) || UNKNOWN.equals(result)) {
                break;
            }
            streak++;
        }
        return streak;
    }

    private int longestStreak(List<String> form, String expected) {
        int longest = 0;
        int current = 0;
        for (String result : form) {
            if (expected.equals(result)) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
        }
        return longest;
    }

    private int count(List<String> form, String expected) {
        return (int) form.stream().filter(expected::equals).count();
    }
}
