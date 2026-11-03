package com.yukai.team.matchservice.opponentanalysis.parser;

import com.yukai.team.matchservice.config.JacksonConfig;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaClientException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlaStandingsHtmlParserTest {
    private final FlaStandingsHtmlParser parser = new FlaStandingsHtmlParser();

    public static String fixture() throws IOException {
        return new ClassPathResource("fla/rankings-representative.html").getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void parsesFourteenTeamsFromCapturedPage() throws IOException {
        String html = new ClassPathResource("fla/rankings-16-season-2.html")
                .getContentAsString(StandardCharsets.UTF_8);
        var standings = parser.parse(html, 16L);
        assertThat(standings).hasSize(14);
        assertThat(standings).extracting(FlaStandingEntryResponse::getTeamId).doesNotHaveDuplicates();
        assertThat(standings).allSatisfy(entry -> assertThat(entry.getChampionnatId()).isEqualTo(16L));
        var yexiao = standings.stream().filter(entry -> entry.getTeamId().equals(270L)).findFirst().orElseThrow();
        assertThat(yexiao.getTeamName()).isEqualTo("YEXIAO PARIS FC");
        // Live standings advanced since the supplied 4-point example; captured 2026-11-03.
        assertThat(yexiao.getPoints()).isEqualTo(20);
        assertThat(yexiao.getWins()).isEqualTo(5);
        assertThat(yexiao.getDraws()).isZero();
        assertThat(yexiao.getLosses()).isZero();
        assertThat(yexiao.getGoalsFor()).isEqualTo(29);
        assertThat(yexiao.getGoalsAgainst()).isEqualTo(6);
        assertThat(yexiao.getForfeits()).isZero();
        assertThat(yexiao.getBonus()).isZero();
        assertThat(yexiao.getForm()).isEmpty();
        var penalized = standings.stream().filter(entry -> entry.getTeamId().equals(63L)).findFirst().orElseThrow();
        assertThat(penalized.getPoints()).isEqualTo(6); // Primary badge, not concatenated "6 (6)".
        assertThat(penalized.getBonus()).isZero(); // Penalty -1 is not legacy Bonus.
    }

    @Test
    void usesPrimaryPointsBadgeAndValidatesSecondaryAnnotation() throws IOException {
        Document doc = Jsoup.parse(fixture());
        Element points = doc.selectFirst("table.rankings tbody tr").child(2);
        points.html("<div><span>4</span><span>(5)</span></div>");
        assertThat(parser.parse(doc.html(), 16L).get(0).getPoints()).isEqualTo(4);
        points.html("<div><span>4</span><span>(invalid)</span></div>");
        rejects(doc.html());
        points.html("<div><span>4</span>invalid</div>");
        rejects(doc.html());
    }

    @Test
    void parsesStandingsAndIgnoresParticipantCardsAndOtherTables() throws IOException {
        // Two representative rows, not a verified 14-team capture of the live page.
        List<FlaStandingEntryResponse> standings = parser.parse(fixture(), 16L);
        assertThat(standings).hasSize(2);
        var entry = standings.get(0);
        assertThat(entry.getTeamName()).isEqualTo("YEXIAO PARIS FC");
        assertThat(entry.getTeamId()).isEqualTo(270L);
        assertThat(entry.getChampionnatId()).isEqualTo(16L);
        assertThat(entry.getPoints()).isEqualTo(4);
        assertThat(entry.getWins()).isEqualTo(1);
        assertThat(entry.getDraws()).isZero();
        assertThat(entry.getLosses()).isZero();
        assertThat(entry.getGoalsFor()).isEqualTo(4);
        assertThat(entry.getGoalsAgainst()).isEqualTo(1);
        assertThat(entry.getForfeits()).isZero();
        assertThat(entry.getBonus()).isZero();
        assertThat(entry.getForm()).isEmpty();
        assertThat(standings.get(1).getBonus()).isZero(); // Penalty 2 is not bonus.
        assertThat(standings.get(1).getPoints()).isEqualTo(-2);
    }

    @Test
    void mapsColumnsByHeaderAndHandlesGroupedNumbers() throws IOException {
        Document doc = Jsoup.parse(fixture());
        Element table = doc.selectFirst("table.rankings");
        // Move Points to the end; its meaning must not depend on the original cell index.
        for (Element row : table.select("tr")) {
            row.appendChild(row.child(2));
        }
        table.selectFirst("tbody tr").children().last().text("1\u202f234");
        assertThat(parser.parse(doc.html(), 42L).get(0).getPoints()).isEqualTo(1234);
        assertThat(parser.parse(doc.html(), 42L).get(0).getChampionnatId()).isEqualTo(42L);
    }

    @Test
    void preservesLegacyJsonNamesAndRoundTripsCompatibilityDefaults() throws Exception {
        var mapper = new JacksonConfig().objectMapper();
        var entry = parser.parse(fixture(), 16L).get(0);
        String json = mapper.writeValueAsString(entry);
        var tree = mapper.readTree(json);
        assertThat(tree.has("IdEquipe")).isTrue();
        assertThat(tree.has("IdChampionnat")).isTrue();
        assertThat(tree.has("Bonus")).isTrue();
        assertThat(tree.has("CinqDernier")).isTrue();
        assertThat(tree.has("teamId")).isFalse();
        var restored = mapper.readValue(json, FlaStandingEntryResponse.class);
        assertThat(restored.getTeamId()).isEqualTo(270L);
        assertThat(restored.getBonus()).isZero();
        assertThat(restored.getForm()).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  \n\t", "<html><h1>Error</h1></html>",
            "<html><form action='/login'><input name='password'></form></html>",
            "<div data-team-name='Team'><a href='/teams/270'>Team</a></div>"})
    void rejectsEmptyOrNonRankingsPages(String html) {
        rejects(html);
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing-id", "invalid-id", "zero-id", "overflow-id", "ambiguous-id",
            "missing-name", "missing-attribute", "short-row", "long-row", "empty-table",
            "missing-column", "duplicate-column", "duplicate-team", "second-table", "unmarked-row"})
    void rejectsMalformedStandings(String change) throws IOException {
        Document doc = Jsoup.parse(fixture());
        Element table = doc.selectFirst("table.rankings");
        Element row = table.selectFirst("tbody tr");
        Element link = row.selectFirst("a");
        switch (change) {
            case "missing-id" -> link.removeAttr("href");
            case "invalid-id" -> link.attr("href", "/teams/abc");
            case "zero-id" -> link.attr("href", "/teams/0");
            case "overflow-id" -> link.attr("href", "/teams/999999999999999999999");
            case "ambiguous-id" -> row.child(1).append("<a href='/teams/272'>Other</a>");
            case "missing-name" -> row.attr("data-team-name", "\u00a0");
            case "missing-attribute" -> row.removeAttr("data-team-name");
            case "short-row" -> row.child(3).remove();
            case "long-row" -> row.append("<td>0</td>");
            case "empty-table" -> table.selectFirst("tbody").empty();
            case "missing-column" -> table.selectFirst("th").remove();
            case "duplicate-column" -> table.selectFirst("th").text("Points");
            case "duplicate-team" -> table.selectFirst("tbody").appendChild(row.clone());
            case "second-table" -> doc.body().appendChild(table.clone());
            case "unmarked-row" -> table.selectFirst("tbody").append("<tr><td>Broken team</td></tr>");
            default -> throw new AssertionError(change);
        }
        rejects(doc.html());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "", "1.5", "1,5", "1 2", "99999999999999"})
    void rejectsInvalidNumericFields(String value) throws IOException {
        Document doc = Jsoup.parse(fixture());
        doc.selectFirst("table.rankings tbody tr").child(2).text(value);
        rejects(doc.html());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 4, 5, 6, 7, 8, 9, 10, 11})
    void validatesEvenFieldsNotRepresentedInDto(int column) throws IOException {
        Document doc = Jsoup.parse(fixture());
        doc.selectFirst("table.rankings tbody tr").child(column).text("invalid");
        rejects(doc.html());
    }

    @Test
    void rejectsNegativeCounters() throws IOException {
        Document doc = Jsoup.parse(fixture());
        doc.selectFirst("table.rankings tbody tr").child(5).text("-1");
        rejects(doc.html());
    }

    private void rejects(String html) {
        assertThatThrownBy(() -> parser.parse(html, 16L))
                .isInstanceOf(FlaClientException.class)
                .extracting("httpStatus", "code")
                .containsExactly(HttpStatus.BAD_GATEWAY, "FLA_INVALID_RESPONSE");
    }
}
