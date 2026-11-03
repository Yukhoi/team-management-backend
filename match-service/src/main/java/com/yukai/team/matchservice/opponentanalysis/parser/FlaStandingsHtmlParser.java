package com.yukai.team.matchservice.opponentanalysis.parser;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaClientException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class FlaStandingsHtmlParser {

    private static final Set<String> COLUMNS = Set.of(
            "position", "equipe", "points", "penalite", "j", "g", "n", "p", "bp", "bc", "diff", "forf");

    public List<FlaStandingEntryResponse> parse(String html, Long championnatId) {
        if (championnatId == null || championnatId <= 0) {
            throw new IllegalArgumentException("championnatId must be positive");
        }
        if (html == null || html.isBlank()) {
            throw invalid("Empty standings HTML");
        }

        // Identify the table by its column headings, never by its position or participant cards.
        List<Element> candidates = Jsoup.parse(html).select("table").stream()
                .filter(table -> {
                    List<String> headers = headers(table);
                    return headers.contains("equipe") && headers.contains("points");
                }).toList();
        if (candidates.size() != 1) {
            throw invalid("Expected one standings table");
        }
        Element table = candidates.get(0);
        List<String> headers = headers(table);
        if (headers.size() != COLUMNS.size() || !new HashSet<>(headers).equals(COLUMNS)) {
            throw invalid("Unexpected standings columns");
        }
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            columns.put(headers.get(i), i);
        }

        List<FlaStandingEntryResponse> result = new ArrayList<>();
        Set<Long> teamIds = new HashSet<>();
        for (Element row : table.select("tbody > tr")) {
            if (row.closest("table") != table) {
                throw invalid("Nested standings table");
            }
            var cells = row.children();
            if (cells.size() != headers.size() || cells.stream().anyMatch(cell -> !cell.normalName().equals("td"))) {
                throw invalid("Malformed standings row");
            }
            String name = whitespace(row.attr("data-team-name"));
            if (name.isBlank()) {
                throw invalid("Missing standings team name");
            }
            Long teamId = teamId(cells.get(columns.get("equipe")));
            if (!teamIds.add(teamId)) {
                throw invalid("Duplicate standings team ID");
            }

            Map<String, Integer> numbers = new HashMap<>();
            for (String column : COLUMNS) {
                if (!column.equals("equipe")) {
                    String text = cells.get(columns.get(column)).text();
                    // The captured FLA page renders no penalty as a dash, not a number.
                    int number = column.equals("points") ? points(cells.get(columns.get(column)))
                            : column.equals("penalite") && whitespace(text).equals("-") ? 0 : number(text);
                    if ((column.equals("position") && number <= 0)
                            || (!Set.of("points", "penalite", "diff").contains(column) && number < 0)) {
                        throw invalid("Invalid standings numeric range");
                    }
                    numbers.put(column, number);
                }
            }
            FlaStandingEntryResponse entry = new FlaStandingEntryResponse();
            entry.setTeamId(teamId);
            entry.setTeamName(name);
            entry.setChampionnatId(championnatId);
            entry.setPoints(numbers.get("points"));
            entry.setWins(numbers.get("g"));
            entry.setDraws(numbers.get("n"));
            entry.setLosses(numbers.get("p"));
            entry.setGoalsFor(numbers.get("bp"));
            entry.setGoalsAgainst(numbers.get("bc"));
            entry.setForfeits(numbers.get("forf"));
            // The new page provides neither legacy Bonus nor CinqDernier. Metrics already
            // support zero bonus and empty form; Penalty must never be treated as Bonus.
            entry.setBonus(0);
            entry.setForm(List.of());
            result.add(entry);
        }
        if (result.isEmpty()) {
            throw invalid("Standings table contains no teams");
        }
        return result;
    }

    private List<String> headers(Element table) {
        return table.select("thead > tr > th").stream()
                .filter(header -> header.closest("table") == table)
                .map(header -> Normalizer.normalize(whitespace(header.text()), Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replace(".", ""))
                .toList();
    }

    private Long teamId(Element teamCell) {
        Set<Long> ids = new HashSet<>();
        for (Element link : teamCell.select("a[href]")) {
            try {
                String path = URI.create(link.attr("href")).getPath();
                if (path != null && path.startsWith("/teams/")) {
                    String id = path.substring("/teams/".length());
                    if (id.endsWith("/")) {
                        id = id.substring(0, id.length() - 1);
                    }
                    if (!id.matches("[0-9]+")) {
                        throw invalid("Invalid standings team ID");
                    }
                    long value = Long.parseLong(id);
                    if (value <= 0) {
                        throw invalid("Invalid standings team ID");
                    }
                    ids.add(value);
                }
            } catch (IllegalArgumentException exception) {
                throw invalid("Invalid standings team link");
            }
        }
        if (ids.size() != 1) {
            throw invalid("Missing or ambiguous standings team ID");
        }
        return ids.iterator().next();
    }

    private int points(Element cell) {
        Element copy = cell.clone();
        Element badge = copy.selectFirst("> div > span");
        if (badge == null) {
            return number(cell.text());
        }
        int points = number(badge.text());
        badge.remove();
        String annotation = whitespace(copy.text());
        // Penalized teams have a separate parenthesized points annotation under the
        // main badge. Validate, but use the displayed primary points unchanged.
        if (!annotation.isEmpty()) {
            if (!annotation.startsWith("(") || !annotation.endsWith(")")) {
                throw invalid("Invalid standings points annotation");
            }
            number(annotation.substring(1, annotation.length() - 1));
        }
        return points;
    }

    private int number(String text) {
        String value = whitespace(text).replace('\u2212', '-');
        if (!value.matches("[+-]?(?:[0-9]+|[0-9]{1,3}(?: [0-9]{3})+)")) {
            throw invalid("Invalid standings numeric field");
        }
        try {
            return Integer.parseInt(value.replace(" ", ""));
        } catch (NumberFormatException exception) {
            throw invalid("Standings numeric field out of range");
        }
    }

    private String whitespace(String text) {
        return text.replaceAll("[\\s\\p{Zs}]+", " ").trim();
    }

    private FlaClientException invalid(String reason) {
        // Static messages only: response content must never enter logs or public errors.
        return new FlaClientException(HttpStatus.BAD_GATEWAY, "FLA_INVALID_RESPONSE", reason);
    }
}
