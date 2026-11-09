package com.yukai.team.matchservice.opponentanalysis.parser;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchesPageResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchResponse.CompetitionType;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchResponse.MatchStatus;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaClientException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class FlaTeamMatchesHtmlParser {
    private static final String SEASON = "[data-testid=calendrier-saison]";
    private static final String MATCH = "[data-testid=rencontre]";
    private static final Pattern DATE = Pattern.compile(
            "^(?:lun|mar|mer|jeu|ven|sam|dim)\\. (\\d{1,2}) ([\\p{L}]+)\\.? (\\d{4})(?: · (\\d{1,2})h(\\d{2}))?(?: ·.*)?$");
    private static final Pattern SCORE = Pattern.compile("^(\\d+) [–-] (\\d+)$");
    private static final Map<String, Integer> MONTHS = Map.ofEntries(
            Map.entry("janv", 1), Map.entry("févr", 2), Map.entry("mars", 3), Map.entry("avr", 4),
            Map.entry("mai", 5), Map.entry("juin", 6), Map.entry("juil", 7), Map.entry("août", 8),
            Map.entry("sept", 9), Map.entry("oct", 10), Map.entry("nov", 11), Map.entry("déc", 12));

    public List<FlaTeamMatchResponse> parse(String html, Long flaTeamId) {
        if (flaTeamId == null || flaTeamId <= 0) {
            throw new IllegalArgumentException("flaTeamId must be positive");
        }
        if (html == null || html.isBlank()) {
            throw invalid("Empty team matches HTML");
        }
        var document = Jsoup.parse(html);
        var canonical = document.select("link[rel=canonical]");
        try {
            if (canonical.size() != 1 || !("/teams/" + flaTeamId)
                    .equals(URI.create(canonical.first().attr("href")).getPath())) {
                throw invalid("Team page identity does not match request");
            }
        } catch (IllegalArgumentException exception) {
            throw invalid("Invalid team page identity");
        }
        var pages = document.select("[x-data=teamPage()]");
        if (pages.size() != 1) {
            throw invalid("Expected one team page");
        }
        Element page = pages.first();
        var headings = page.select("h1");
        var seasons = page.select(SEASON);
        if (headings.size() != 1 || headings.first().text().isBlank() || seasons.size() != 1) {
            throw invalid("Missing team heading or season calendar");
        }
        String teamName = whitespace(headings.first().text());
        Element season = seasons.first();
        var cards = season.select(MATCH);
        if (cards.isEmpty()) {
            // An empty calendar is valid only when explicitly represented. Do not silently
            // turn an upstream selector/layout change into a team with no matches.
            if (season.childrenSize() == 0 && season.text().isBlank()) {
                return List.of();
            }
            throw invalid("Season calendar contains no recognizable matches");
        }
        Map<MatchKey, FlaTeamMatchResponse> matches = new LinkedHashMap<>();
        for (Element card : cards) {
            if (card.parent() != season) {
                throw invalid("Unexpected season match container");
            }
            FlaTeamMatchResponse match = parseCard(card, flaTeamId, teamName);
            if (match == null) {
                continue; // Only records with a known date and opponent can enter recent form.
            }
            MatchKey key = new MatchKey(match.matchDate(), match.kickoffTime(), match.homeTeamName(),
                    match.awayTeamName(), match.competitionType(), match.competitionName(), match.competitionRound());
            FlaTeamMatchResponse previous = matches.putIfAbsent(key, match);
            if (previous != null && !previous.equals(match)) {
                throw invalid("Conflicting duplicate season matches");
            }
        }
        return List.copyOf(matches.values());
    }

    public FlaTeamMatchesPageResponse parsePage(String html, Long teamId) {
        List<FlaTeamMatchResponse> matches = parse(html, teamId);
        var page = Jsoup.parse(html).selectFirst("[x-data=teamPage()]");
        java.util.Set<Long> championshipIds = new java.util.HashSet<>();
        for (Element link : page.select("a[href]")) {
            try {
                String path = URI.create(link.attr("href")).getPath();
                if (path != null && path.matches("/championships/[1-9][0-9]*")) {
                    championshipIds.add(Long.parseLong(path.substring("/championships/".length())));
                }
            } catch (IllegalArgumentException exception) {
                throw invalid("Invalid team championship link");
            }
        }
        Long championshipId = championshipIds.size() == 1 ? championshipIds.iterator().next() : null;
        // Both captured pages lack a season identifier. Never infer one from match years,
        // a requested mapping, the current date, or the default standings season.
        return new FlaTeamMatchesPageResponse(
                teamId, championshipId, null, matches);
    }

    private FlaTeamMatchResponse parseCard(Element card, Long teamId, String teamName) {
        if (card.childrenSize() != 2) {
            throw invalid("Unexpected match card layout");
        }
        Element metadata = card.child(0);
        Element details = card.child(1);
        if (metadata.childrenSize() < 3 || details.childrenSize() != 2
                || !metadata.children().stream().allMatch(element -> element.normalName().equals("span"))) {
            throw invalid("Unexpected match metadata layout");
        }
        Element description = details.child(0);
        Element outcome = details.child(1);
        var paragraphs = description.select("> p");
        if (paragraphs.size() != 2) {
            throw invalid("Missing match description fields");
        }
        String venue = whitespace(metadata.children().last().text());
        boolean home;
        String prefix;
        if (venue.equals("Domicile")) {
            home = true;
            prefix = "Reçoit ";
        } else if (venue.equals("Extérieur")) {
            home = false;
            prefix = "Se déplace chez ";
        } else {
            throw invalid("Unknown match home/away marker");
        }
        String opponentText = whitespace(paragraphs.get(0).text());
        if (opponentText.isBlank() || opponentText.equals(prefix.trim())) {
            return null;
        }
        if (!opponentText.startsWith(prefix)) {
            throw invalid("Match opponent does not match home/away marker");
        }
        String opponent = opponentText.substring(prefix.length()).trim();
        if (opponent.isBlank()) {
            return null;
        }
        // ownText excludes the nested stadium link. Years are explicitly present in real HTML.
        String dateText = whitespace(paragraphs.get(1).ownText());
        if (dateText.isBlank() || dateText.equals("·")) {
            return null;
        }
        Matcher date = DATE.matcher(dateText);
        if (!date.matches() || !MONTHS.containsKey(date.group(2))) {
            throw invalid("Invalid match date or missing explicit year");
        }
        LocalDate matchDate;
        LocalTime kickoffTime;
        try {
            matchDate = LocalDate.of(Integer.parseInt(date.group(3)), MONTHS.get(date.group(2)),
                    Integer.parseInt(date.group(1)));
            kickoffTime = date.group(4) == null ? null
                    : LocalTime.of(Integer.parseInt(date.group(4)), Integer.parseInt(date.group(5)));
        } catch (DateTimeException exception) {
            throw invalid("Invalid match date or time");
        }

        Integer ownScore = null;
        Integer opponentScore = null;
        MatchStatus status = MatchStatus.UNKNOWN;
        var scores = outcome.select("[data-testid=rencontre-score]");
        if (scores.size() > 1) {
            throw invalid("Ambiguous match score");
        }
        if (!scores.isEmpty()) {
            Element scoreElement = scores.first();
            Matcher score = SCORE.matcher(whitespace(scoreElement.text()));
            if (score.matches()) {
                try {
                    ownScore = Integer.valueOf(score.group(1));
                    opponentScore = Integer.valueOf(score.group(2));
                } catch (NumberFormatException exception) {
                    throw invalid("Match score out of range");
                }
                Element marker = scoreElement.nextElementSibling();
                String result = marker == null ? "" : whitespace(marker.text());
                String expected = ownScore > opponentScore ? "V" : ownScore < opponentScore ? "D" : "N";
                if (List.of("V", "N", "D").contains(result)) {
                    if (!result.equals(expected)) {
                        throw invalid("Match score conflicts with result marker");
                    }
                    status = MatchStatus.COMPLETED;
                }
            } else if (!whitespace(scoreElement.text()).matches("[–-]?")) {
                throw invalid("Invalid match score");
            }
        } else if (whitespace(outcome.text()).equals("À jouer")) {
            status = MatchStatus.SCHEDULED;
        }
        String category = whitespace(metadata.child(0).text());
        CompetitionType competitionType = switch (category) {
            case "Championnat" -> CompetitionType.LEAGUE;
            case "Coupe" -> CompetitionType.CUP;
            default -> CompetitionType.OTHER;
        };
        String competitionName = whitespace(metadata.child(1).text());
        if (category.isBlank() || competitionName.isBlank()) {
            throw invalid("Missing match competition");
        }
        List<String> round = new ArrayList<>();
        for (int i = 2; i < metadata.childrenSize() - 1; i++) {
            round.add(whitespace(metadata.child(i).text()));
        }
        // FLA renders own-team goals first even on away pages, NOT home goals first.
        return new FlaTeamMatchResponse(matchDate, kickoffTime,
                home ? teamId : null, home ? teamName : opponent,
                home ? null : teamId, home ? opponent : teamName,
                home ? ownScore : opponentScore, home ? opponentScore : ownScore,
                competitionType, competitionName, round.isEmpty() ? null : String.join(" ", round), status);
    }

    private String whitespace(String text) {
        return text.replaceAll("[\\s\\p{Zs}]+", " ").trim();
    }

    private FlaClientException invalid(String reason) {
        return new FlaClientException(HttpStatus.BAD_GATEWAY, "FLA_INVALID_RESPONSE", reason);
    }

    private record MatchKey(LocalDate date, LocalTime time, String home, String away,
                            CompetitionType type, String competition, String round) { }
}
