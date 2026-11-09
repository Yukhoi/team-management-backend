package com.yukai.team.matchservice.opponentanalysis.client;

import com.yukai.team.matchservice.opponentanalysis.config.FlaClientConfig;
import com.yukai.team.matchservice.opponentanalysis.config.FlaProperties;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaClientException;
import com.yukai.team.matchservice.opponentanalysis.parser.FlaStandingsHtmlParser;
import com.yukai.team.matchservice.opponentanalysis.parser.FlaTeamMatchesHtmlParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FlaTeamMatchesClientTest {
    private MockRestServiceServer server;
    private FlaClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = new FlaClientConfig().flaRestClient(new FlaProperties()).mutate();
        server = MockRestServiceServer.bindTo(builder).build();
        client = client(builder.build());
    }

    @Test
    void requestsCapturedTeamPageWithProductionEncodingAndHeaders() {
        server.expect(requestTo("https://football-loisir-amateur.fr/teams/63"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE))
                .andRespond(withSuccess(new ClassPathResource("fla/team-63.html"), MediaType.TEXT_HTML));
        assertThat(client.getTeamMatches(63L)).hasSize(29);
        server.verify();
    }

    @ParameterizedTest
    @CsvSource({"63, 5, 5", "63, 10, 7", "270, 5, 5", "270, 10, 6"})
    void recentMatchesUseOnlyCompletedGamesAndReturnActualCount(long teamId, int limit, int count) {
        server.expect(requestTo("https://football-loisir-amateur.fr/teams/" + teamId))
                .andRespond(withSuccess(new ClassPathResource("fla/team-" + teamId + ".html"), MediaType.TEXT_HTML));
        var matches = client.getRecentCompletedMatches(teamId, limit);
        assertThat(matches).hasSize(count).allSatisfy(match -> {
            assertThat(match.hasCompletedScore()).isTrue();
            assertThat(match.matchDate()).isBeforeOrEqualTo(LocalDate.now(ZoneId.of("Europe/Paris")));
        });
        assertThat(matches).extracting(m -> m.matchDate()).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(matches.get(0).matchDate()).isEqualTo(LocalDate.of(2026, 11, 6));
        server.verify();
    }

    @Test
    void fewerThanFiveReturnsOnlyAvailableDistinctMatches() throws IOException {
        Document doc = oneMatch();
        Element card = firstCard(doc);
        card.parent().appendChild(card.clone());
        expectHtml(doc.html());
        assertThat(client.getRecentCompletedMatches(63L, 5)).hasSize(1);
        server.verify();
    }

    @Test
    void excludesFutureDatedMatchEvenWithScoreAndResult() throws IOException {
        Document doc = oneMatch();
        LocalDate future = LocalDate.now(ZoneId.of("Europe/Paris")).plusYears(1);
        firstCard(doc).select("p").get(1).text(future.format(DateTimeFormatter.ofPattern("EEE d MMM uuuu", Locale.FRENCH)));
        expectHtml(doc.html());
        assertThat(client.getRecentCompletedMatches(63L, 10)).isEmpty();
        server.verify();
    }

    @Test
    void excludesUnknownStateAndUndatedMatches() throws IOException {
        Document doc = oneMatch();
        Element card = firstCard(doc);
        Element undated = card.clone();
        undated.select("p").get(1).empty();
        card.parent().appendChild(undated);
        card.selectFirst("[data-testid=rencontre-score]").nextElementSibling().text("Inconnu");
        expectHtml(doc.html());
        assertThat(client.getRecentCompletedMatches(63L, 5)).isEmpty();
        server.verify();
    }

    @Test
    void emptyCalendarIsDistinctFromNetworkFailure() throws IOException {
        Document doc = oneMatch();
        doc.selectFirst("[data-testid=calendrier-saison]").empty();
        expectHtml(doc.html());
        assertThat(client.getTeamMatches(63L)).isEmpty();
        server.verify();
    }

    @ParameterizedTest
    @CsvSource({"404, FLA_INVALID_REQUEST", "500, FLA_UNAVAILABLE", "503, FLA_UNAVAILABLE", "302, FLA_HTTP_ERROR"})
    void mapsHttpErrorsWithoutRetainingResponseBodies(int status, String code) {
        server.expect(requestTo("https://football-loisir-amateur.fr/teams/63"))
                .andRespond(withStatus(HttpStatus.valueOf(status)).body("PRIVATE RESPONSE CONTENT"));
        assertThatThrownBy(() -> client.getTeamMatches(63L)).isInstanceOf(FlaClientException.class)
                .hasNoCause().hasMessageNotContaining("PRIVATE RESPONSE CONTENT")
                .extracting("httpStatus", "code").containsExactly(HttpStatus.BAD_GATEWAY, code);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-html", "<html><h1>Login</h1></html>"})
    void rejectsInvalidSuccessfulHtml(String html) {
        expectHtml(html);
        assertFailure(client, HttpStatus.BAD_GATEWAY, "FLA_INVALID_RESPONSE");
        server.verify();
    }

    @Test
    void mapsTimeoutAndIoFailure() {
        assertFailure(failingClient(new SocketTimeoutException("timeout")), HttpStatus.GATEWAY_TIMEOUT, "FLA_TIMEOUT");
        assertFailure(failingClient(new IOException("connection reset")), HttpStatus.BAD_GATEWAY, "FLA_IO_ERROR");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rejectsInvalidTeamIdsWithoutHttp(Long id) {
        assertThatThrownBy(() -> client.getTeamMatches(id)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client.getRecentCompletedMatches(id, 5)).isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonpositiveLimitWithoutHttp(int limit) {
        assertThatThrownBy(() -> client.getRecentCompletedMatches(63L, limit)).isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    private FlaClient failingClient(IOException exception) {
        return client(RestClient.builder().baseUrl("https://fla.test")
                .requestFactory((uri, method) -> { throw exception; }).build());
    }

    private FlaClient client(RestClient restClient) {
        return new FlaClientImpl(restClient, new FlaStandingsHtmlParser(), new FlaTeamMatchesHtmlParser());
    }

    private void assertFailure(FlaClient target, HttpStatus status, String code) {
        assertThatThrownBy(() -> target.getTeamMatches(63L)).isInstanceOf(FlaClientException.class)
                .extracting("httpStatus", "code").containsExactly(status, code);
    }

    private void expectHtml(String html) {
        server.expect(requestTo("https://football-loisir-amateur.fr/teams/63")).andRespond(withSuccess(html, MediaType.TEXT_HTML));
    }

    private Document oneMatch() throws IOException {
        Document doc = Jsoup.parse(new ClassPathResource("fla/team-63.html").getContentAsString(StandardCharsets.UTF_8));
        doc.select("[data-testid=calendrier-saison] > [data-testid=rencontre]").stream().skip(1).forEach(Element::remove);
        return doc;
    }

    private Element firstCard(Document doc) {
        return doc.selectFirst("[data-testid=calendrier-saison] > [data-testid=rencontre]");
    }
}
