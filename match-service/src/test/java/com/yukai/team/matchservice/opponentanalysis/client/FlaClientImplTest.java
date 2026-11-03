package com.yukai.team.matchservice.opponentanalysis.client;

import com.yukai.team.matchservice.opponentanalysis.config.FlaProperties;
import com.yukai.team.matchservice.opponentanalysis.config.FlaClientConfig;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaClientException;
import com.yukai.team.matchservice.opponentanalysis.parser.FlaStandingsHtmlParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FlaClientImplTest {
    private MockRestServiceServer server;
    private FlaClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = new FlaClientConfig().flaRestClient(new FlaProperties()).mutate();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FlaClientImpl(builder.build(), new FlaStandingsHtmlParser());
    }

    @Test
    void getStandingsRequestsHtmlWithGetAndSeasonQuery() {
        server.expect(requestTo("https://football-loisir-amateur.fr/championships/16/rankings?season=2"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE))
                .andExpect(content().string(""))
                .andRespond(withSuccess(new ClassPathResource("fla/rankings-representative.html"), MediaType.TEXT_HTML));

        var standings = client.getStandings(16L, 2L);
        assertThat(standings).hasSize(2);
        assertThat(standings.get(0).getTeamId()).isEqualTo(270L);
        assertThat(standings.get(0).getChampionnatId()).isEqualTo(16L);
        server.verify();
    }

    @Test
    void getStandingsParsesCapturedPageUsingProductionClientConfiguration() {
        server.expect(requestTo("https://football-loisir-amateur.fr/championships/16/rankings?season=2"))
                .andRespond(withSuccess(new ClassPathResource("fla/rankings-16-season-2.html"), MediaType.TEXT_HTML));
        var standings = client.getStandings(16L, 2L);
        assertThat(standings).hasSize(14);
        assertThat(standings.get(0).getTeamId()).isEqualTo(270L);
        assertThat(standings.get(0).getPoints()).isEqualTo(20);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-html", "[]", "<html><h1>Service unavailable</h1></html>",
            "<html><form action='/login'><input name='password'></form></html>"})
    void getStandingsRejectsInvalidSuccessfulResponses(String body) {
        server.expect(requestTo("https://football-loisir-amateur.fr/championships/16/rankings?season=2"))
                .andRespond(withSuccess(body, MediaType.TEXT_HTML));
        assertFailure(client, HttpStatus.BAD_GATEWAY, "FLA_INVALID_RESPONSE");
        server.verify();
    }

    @ParameterizedTest
    @CsvSource({"400, FLA_INVALID_REQUEST", "403, FLA_INVALID_REQUEST", "404, FLA_INVALID_REQUEST",
            "500, FLA_UNAVAILABLE", "503, FLA_UNAVAILABLE", "302, FLA_HTTP_ERROR"})
    void getStandingsMapsHttpErrorsWithoutRetainingResponseContent(int status, String code) {
        server.expect(requestTo("https://football-loisir-amateur.fr/championships/16/rankings?season=2"))
                .andRespond(withStatus(HttpStatus.valueOf(status))
                        .body("<html>PRIVATE RESPONSE CONTENT</html>").contentType(MediaType.TEXT_HTML));
        assertThatThrownBy(() -> client.getStandings(16L, 2L))
                .isInstanceOf(FlaClientException.class)
                .hasNoCause()
                .hasMessageNotContaining("PRIVATE RESPONSE CONTENT")
                .extracting("httpStatus", "code")
                .containsExactly(HttpStatus.BAD_GATEWAY, code);
        server.verify();
    }

    @Test
    void getStandingsMapsTimeout() {
        assertFailure(failingClient(new SocketTimeoutException("timeout")),
                HttpStatus.GATEWAY_TIMEOUT, "FLA_TIMEOUT");
    }

    @Test
    void getStandingsMapsIoErrors() {
        assertFailure(failingClient(new IOException("connection reset")),
                HttpStatus.BAD_GATEWAY, "FLA_IO_ERROR");
    }

    @Test
    void configurationDefaultsRetainTimeoutsAndTtl() {
        FlaProperties properties = new FlaProperties();
        assertThat(properties.getBaseUrl()).isEqualTo("https://football-loisir-amateur.fr");
        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(20));
        assertThat(properties.getSnapshotTtl()).isEqualTo(Duration.ofHours(6));
    }

    private FlaClient failingClient(IOException exception) {
        RestClient restClient = RestClient.builder().baseUrl("https://fla.test")
                .requestFactory((uri, method) -> { throw exception; }).build();
        return new FlaClientImpl(restClient, new FlaStandingsHtmlParser());
    }

    private void assertFailure(FlaClient target, HttpStatus status, String code) {
        assertThatThrownBy(() -> target.getStandings(16L, 2L))
                .isInstanceOf(FlaClientException.class)
                .extracting("httpStatus", "code")
                .containsExactly(status, code);
    }
}
