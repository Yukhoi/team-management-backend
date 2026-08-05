package com.yukai.team.matchservice.opponentanalysis.client;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingsRequest;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaClientException;
import com.yukai.team.matchservice.opponentanalysis.util.FlaRequestKeyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class FlaClientImpl implements FlaClient {

    private static final Logger log = LoggerFactory.getLogger(FlaClientImpl.class);
    private static final ParameterizedTypeReference<List<FlaStandingEntryResponse>> STANDINGS_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final MediaType FLA_JSON = new MediaType("application", "json", StandardCharsets.UTF_8);

    private final RestClient restClient;

    public FlaClientImpl(@Qualifier("flaRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<FlaStandingEntryResponse> getStandings(Long championnatId, Long saisonId) {
        String requestKey = FlaRequestKeyBuilder.buildClassementRequestKey(championnatId, saisonId);
        log.info("Fetching FLA standings, championnatId={}, saisonId={}", championnatId, saisonId);

        try {
            List<FlaStandingEntryResponse> standings = restClient.post()
                    .uri("/Home/GetClassement/{requestKey}", requestKey)
                    .contentType(FLA_JSON)
                    .body(new FlaStandingsRequest(championnatId, saisonId))
                    .retrieve()
                    .body(STANDINGS_TYPE);

            if (standings == null) {
                throw new FlaClientException(
                        HttpStatus.BAD_GATEWAY,
                        "FLA_EMPTY_RESPONSE",
                        "FLA returned empty standings response"
                );
            }

            log.info(
                    "Fetched FLA standings, championnatId={}, saisonId={}, teamCount={}",
                    championnatId,
                    saisonId,
                    standings.size()
            );
            return standings;
        } catch (FlaClientException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw mapStatusException(exception);
        } catch (ResourceAccessException exception) {
            if (isTimeout(exception)) {
                throw new FlaClientException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "FLA_TIMEOUT",
                        "FLA request timed out",
                        exception
                );
            }
            throw new FlaClientException(
                    HttpStatus.BAD_GATEWAY,
                    "FLA_IO_ERROR",
                    "Failed to call FLA service",
                    exception
            );
        } catch (RestClientException exception) {
            throw new FlaClientException(
                    HttpStatus.BAD_GATEWAY,
                    "FLA_INVALID_RESPONSE",
                    "FLA response could not be parsed",
                    exception
            );
        }
    }

    private FlaClientException mapStatusException(RestClientResponseException exception) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status != null && status.is4xxClientError()) {
            return new FlaClientException(
                    HttpStatus.BAD_GATEWAY,
                    "FLA_INVALID_REQUEST",
                    "FLA rejected standings request",
                    exception
            );
        }
        if (status != null && status.is5xxServerError()) {
            return new FlaClientException(
                    HttpStatus.BAD_GATEWAY,
                    "FLA_UNAVAILABLE",
                    "FLA service is unavailable",
                    exception
            );
        }
        return new FlaClientException(
                HttpStatus.BAD_GATEWAY,
                "FLA_HTTP_ERROR",
                "FLA service returned an unexpected HTTP status",
                exception
        );
    }

    private boolean isTimeout(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
