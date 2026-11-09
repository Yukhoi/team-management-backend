package com.yukai.team.matchservice.opponentanalysis.parser;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchResponse.CompetitionType;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchResponse.MatchResult;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchResponse.MatchStatus;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaClientException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlaTeamMatchesHtmlParserTest {
    private final FlaTeamMatchesHtmlParser parser = new FlaTeamMatchesHtmlParser();

    @Test
    void parsesCapturedEmpireSeasonWithoutOtherSectionsAndPreservesCompetitions() throws IOException {
        var matches = parser.parse(fixture(63), 63L);
        assertThat(matches).hasSize(29);
        assertThat(matches.stream().filter(FlaTeamMatchResponse::hasCompletedScore)).hasSize(7);
        assertThat(matches.stream().filter(m -> m.competitionType() == CompetitionType.CUP)).hasSize(3);
        var home = matches.get(6);
        assertThat(home.matchDate()).isEqualTo(LocalDate.of(2026, 11, 6));
        assertThat(home.kickoffTime()).isEqualTo(LocalTime.of(19, 30));
        assertThat(home.homeTeamId()).isEqualTo(63L);
        assertThat(home.homeTeamName()).isEqualTo("18 EMPIRE");
        assertThat(home.awayTeamId()).isNull();
        assertThat(home.awayTeamName()).isEqualTo("YEXIAO PARIS FC");
        assertThat(home.homeScore()).isEqualTo(3);
        assertThat(home.awayScore()).isEqualTo(2);
        assertThat(home.competitionType()).isEqualTo(CompetitionType.LEAGUE);
        assertThat(home.competitionName()).isEqualTo("Foot à 7 - Vendredi - 2ème division");
        assertThat(home.competitionRound()).isEqualTo("· Journée 3 · Aller");
        assertThat(home.status()).isEqualTo(MatchStatus.COMPLETED);
        assertThat(home.resultFor(63L)).isEqualTo(MatchResult.WIN);
        assertThat(home.resultFor(null)).isNull();
        assertThat(home.resultFor(270L)).isNull(); // Opponent ID is not in this page.
    }

    @Test
    void crossChecksSameMatchOnBothCapturedPagesAndConvertsAwayScores() throws IOException {
        var home = parser.parse(fixture(63), 63L).get(6);
        var away = parser.parse(fixture(270), 270L).get(5);
        assertThat(away.homeTeamId()).isNull();
        assertThat(away.awayTeamId()).isEqualTo(270L);
        assertThat(away.homeTeamName()).isEqualTo(home.homeTeamName());
        assertThat(away.awayTeamName()).isEqualTo(home.awayTeamName());
        assertThat(away.homeScore()).isEqualTo(home.homeScore()).isEqualTo(3);
        assertThat(away.awayScore()).isEqualTo(home.awayScore()).isEqualTo(2);
        assertThat(away.resultFor(270L)).isEqualTo(MatchResult.LOSS);
        var awayWin = parser.parse(fixture(270), 270L).get(0);
        assertThat(awayWin.homeScore()).isEqualTo(1);
        assertThat(awayWin.awayScore()).isEqualTo(4);
        assertThat(awayWin.resultFor(270L)).isEqualTo(MatchResult.WIN);
    }

    @Test
    void handlesDrawsIncludingAnActualZeroScore() throws IOException {
        var draw = parser.parse(fixture(63), 63L).get(0);
        assertThat(draw.homeScore()).isEqualTo(2);
        assertThat(draw.awayScore()).isEqualTo(2);
        assertThat(draw.resultFor(63L)).isEqualTo(MatchResult.DRAW);
        Document doc = oneMatch();
        firstCard(doc).selectFirst("[data-testid=rencontre-score]").text("0 – 0");
        assertThat(parser.parse(doc.html(), 63L).get(0).resultFor(63L)).isEqualTo(MatchResult.DRAW);
    }

    @Test
    void recognizesScheduledMatchesAndExplicitYearsAcrossSeasonBoundary() throws IOException {
        var matches = parser.parse(fixture(63), 63L);
        var scheduled = matches.get(7);
        assertThat(scheduled.status()).isEqualTo(MatchStatus.SCHEDULED);
        assertThat(scheduled.homeScore()).isNull();
        assertThat(scheduled.awayScore()).isNull();
        assertThat(scheduled.resultFor(63L)).isNull();
        assertThat(matches.get(13).matchDate()).isEqualTo(LocalDate.of(2027, 1, 8));
        assertThat(matches.get(28).matchDate()).isEqualTo(LocalDate.of(2027, 6, 4));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "–", "-"})
    void missingScoreStaysNullAndUnknown(String score) throws IOException {
        Document doc = oneMatch();
        firstCard(doc).selectFirst("[data-testid=rencontre-score]").text(score);
        var match = parser.parse(doc.html(), 63L).get(0);
        assertThat(match.homeScore()).isNull();
        assertThat(match.awayScore()).isNull();
        assertThat(match.status()).isEqualTo(MatchStatus.UNKNOWN);
        assertThat(match.resultFor(63L)).isNull();
    }

    @Test
    void removedScoreCannotBeCompletedFromResultBadgeAlone() throws IOException {
        Document doc = oneMatch();
        firstCard(doc).selectFirst("[data-testid=rencontre-score]").remove();
        var match = parser.parse(doc.html(), 63L).get(0);
        assertThat(match.status()).isEqualTo(MatchStatus.UNKNOWN);
        assertThat(match.hasCompletedScore()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Reporté", "Annulé", "En cours"})
    void unverifiedStatusIsNeverInferredAsCompleted(String status) throws IOException {
        Document doc = oneMatch();
        firstCard(doc).selectFirst("[data-testid=rencontre-score]").nextElementSibling().text(status);
        var match = parser.parse(doc.html(), 63L).get(0);
        assertThat(match.status()).isEqualTo(MatchStatus.UNKNOWN);
        assertThat(match.resultFor(63L)).isNull();
    }

    @Test
    void skipsMissingDateAndMissingOpponent() throws IOException {
        Document doc = oneMatch();
        firstCard(doc).select("p").get(1).empty();
        assertThat(parser.parse(doc.html(), 63L)).isEmpty();
        doc = oneMatch();
        firstCard(doc).select("p").get(0).text("Se déplace chez ");
        assertThat(parser.parse(doc.html(), 63L)).isEmpty();
    }

    @Test
    void acceptsAnExplicitlyEmptyCalendar() throws IOException {
        Document doc = oneMatch();
        doc.selectFirst("[data-testid=calendrier-saison]").empty();
        assertThat(parser.parse(doc.html(), 63L)).isEmpty();
    }

    @Test
    void deduplicatesRepeatedSeasonCardsAndRejectsConflictingCopies() throws IOException {
        Document doc = oneMatch();
        Element duplicate = firstCard(doc).clone();
        doc.selectFirst("[data-testid=calendrier-saison]").appendChild(duplicate);
        assertThat(parser.parse(doc.html(), 63L)).hasSize(1);
        duplicate.selectFirst("[data-testid=rencontre-score]").text("3 – 3");
        assertInvalid(doc.html());
    }

    @Test
    void supportsAccentsAmpersandsApostrophesAndOtherCompetitionTypes() throws IOException {
        Document doc = oneMatch();
        doc.selectFirst("[x-data=teamPage()] h1").text("Équipe d'été & 巴黎");
        firstCard(doc).select("p").get(0).text("Se déplace chez L'Étoile & Amis");
        firstCard(doc).child(0).child(0).text("Amical");
        var match = parser.parse(doc.html(), 63L).get(0);
        assertThat(match.awayTeamName()).isEqualTo("Équipe d'été & 巴黎");
        assertThat(match.homeTeamName()).isEqualTo("L'Étoile & Amis");
        assertThat(match.competitionType()).isEqualTo(CompetitionType.OTHER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ven. 25 sept.", "ven. 31 févr. 2026", "ven. 25 sept. 2026 · 25h99"})
    void rejectsUnreliableDateRatherThanAssumingCurrentYear(String date) throws IOException {
        Document doc = oneMatch();
        firstCard(doc).select("p").get(1).text(date);
        assertInvalid(doc.html());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2 –", "-1 – 2", "unknown", "999999999999 – 2"})
    void rejectsMalformedScores(String score) throws IOException {
        Document doc = oneMatch();
        firstCard(doc).selectFirst("[data-testid=rencontre-score]").text(score);
        assertInvalid(doc.html());
    }

    @Test
    void rejectsChangedStructureAndIdentityMismatch() throws IOException {
        for (String selector : List.of("[data-testid=calendrier-saison]", "[x-data=teamPage()] h1", "link[rel=canonical]")) {
            Document doc = oneMatch();
            doc.select(selector).remove();
            assertInvalid(doc.html());
        }
        Document doc = oneMatch();
        firstCard(doc).removeAttr("data-testid");
        assertInvalid(doc.html());
        doc = oneMatch();
        firstCard(doc).select("p").get(0).remove();
        assertInvalid(doc.html());
        doc = oneMatch();
        firstCard(doc).child(0).children().last().text("Inconnu");
        assertInvalid(doc.html());
        assertInvalid(fixture(270));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "not-html", "<html><h1>Login</h1></html>"})
    void rejectsInvalidHtml(String html) {
        assertInvalid(html);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rejectsInvalidTeamId(Long teamId) {
        assertThatThrownBy(() -> parser.parse("", teamId)).isInstanceOf(IllegalArgumentException.class);
    }

    private Document oneMatch() throws IOException {
        Document doc = Jsoup.parse(fixture(63));
        doc.select("[data-testid=calendrier-saison] > [data-testid=rencontre]").stream().skip(1).forEach(Element::remove);
        return doc;
    }

    private Element firstCard(Document doc) {
        return doc.selectFirst("[data-testid=calendrier-saison] > [data-testid=rencontre]");
    }

    private String fixture(long id) throws IOException {
        return new ClassPathResource("fla/team-" + id + ".html").getContentAsString(StandardCharsets.UTF_8);
    }

    private void assertInvalid(String html) {
        assertThatThrownBy(() -> parser.parse(html, 63L)).isInstanceOf(FlaClientException.class)
                .extracting("httpStatus", "code").containsExactly(HttpStatus.BAD_GATEWAY, "FLA_INVALID_RESPONSE");
    }
}
