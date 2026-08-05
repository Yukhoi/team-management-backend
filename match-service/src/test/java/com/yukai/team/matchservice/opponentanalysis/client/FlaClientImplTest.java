package com.yukai.team.matchservice.opponentanalysis.client;

import com.yukai.team.matchservice.opponentanalysis.exception.FlaClientException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FlaClientImplTest {

    private static final MediaType FLA_JSON = new MediaType("application", "json", StandardCharsets.UTF_8);

    @Test
    void getStandingsParsesNormalJson() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://fla.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://fla.test/Home/GetClassement/136515"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(FLA_JSON))
                .andExpect(content().json("{\"championnatId\":1365,\"saisonId\":15}"))
                .andRespond(withSuccess("""
                        [{
                          "IdEquipe": 6580,
                          "Nom": "YEXIAO PARIS FC",
                          "Points": 46,
                          "IdChampionnat": 1365,
                          "Victoire": 6,
                          "Egalite": 3,
                          "Defaite": 17,
                          "Bp": 63,
                          "Bc": 134,
                          "Bonus": -1,
                          "Forfait": 0,
                          "CinqDernier": ["won", "lost"],
                          "Ignored": "value"
                        }]
                        """, MediaType.APPLICATION_JSON));

        var standings = new FlaClientImpl(builder.build()).getStandings(1365L, 15L);

        assertThat(standings).hasSize(1);
        assertThat(standings.get(0).getTeamId()).isEqualTo(6580L);
        assertThat(standings.get(0).getTeamName()).isEqualTo("YEXIAO PARIS FC");
        assertThat(standings.get(0).getForm()).containsExactly("won", "lost");
        server.verify();
    }

    @Test
    void getStandingsAllowsEmptyArray() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://fla.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://fla.test/Home/GetClassement/136515"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(new FlaClientImpl(builder.build()).getStandings(1365L, 15L)).isEmpty();
        server.verify();
    }

    @Test
    void getStandingsMapsBadRequest() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://fla.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://fla.test/Home/GetClassement/136515"))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> new FlaClientImpl(builder.build()).getStandings(1365L, 15L))
                .isInstanceOf(FlaClientException.class)
                .extracting("httpStatus", "code")
                .containsExactly(HttpStatus.BAD_GATEWAY, "FLA_INVALID_REQUEST");
        server.verify();
    }

    @Test
    void getStandingsMapsServerError() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://fla.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://fla.test/Home/GetClassement/136515"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> new FlaClientImpl(builder.build()).getStandings(1365L, 15L))
                .isInstanceOf(FlaClientException.class)
                .extracting("httpStatus", "code")
                .containsExactly(HttpStatus.BAD_GATEWAY, "FLA_UNAVAILABLE");
        server.verify();
    }

    @Test
    void getStandingsMapsTimeout() {
        RestClient restClient = RestClient.builder()
                .baseUrl("https://fla.test")
                .requestFactory((uri, method) -> {
                    throw new SocketTimeoutException("timeout");
                })
                .build();

        assertThatThrownBy(() -> new FlaClientImpl(restClient).getStandings(1365L, 15L))
                .isInstanceOf(FlaClientException.class)
                .extracting("httpStatus", "code")
                .containsExactly(HttpStatus.GATEWAY_TIMEOUT, "FLA_TIMEOUT");
    }

    @Test
    void getStandingsMapsInvalidJson() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://fla.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://fla.test/Home/GetClassement/136515"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> new FlaClientImpl(builder.build()).getStandings(1365L, 15L))
                .isInstanceOf(FlaClientException.class)
                .extracting("httpStatus", "code")
                .containsExactly(HttpStatus.BAD_GATEWAY, "FLA_INVALID_RESPONSE");
        server.verify();
    }

    @Test
    void getStandingsMapsNullOrEmptyResponse() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://fla.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(once(), requestTo("https://fla.test/Home/GetClassement/136515"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> new FlaClientImpl(builder.build()).getStandings(1365L, 15L))
                .isInstanceOf(FlaClientException.class)
                .extracting("httpStatus", "code")
                .containsExactly(HttpStatus.BAD_GATEWAY, "FLA_EMPTY_RESPONSE");
        server.verify();
    }
}
