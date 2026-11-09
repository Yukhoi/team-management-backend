package com.yukai.team.matchservice.opponentanalysis.client;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaStandingEntryResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchResponse;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaClientException;
import com.yukai.team.matchservice.opponentanalysis.parser.FlaStandingsHtmlParser;
import com.yukai.team.matchservice.opponentanalysis.parser.FlaTeamMatchesHtmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMatchesPageResponse;

import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

@Component
public class FlaClientImpl implements FlaClient {

    private static final Logger log = LoggerFactory.getLogger(FlaClientImpl.class);
    private final RestClient restClient;
    private final FlaStandingsHtmlParser parser;
    private final FlaTeamMatchesHtmlParser teamMatchesParser;

    public FlaClientImpl(@Qualifier("flaRestClient") RestClient restClient, FlaStandingsHtmlParser parser,
                         FlaTeamMatchesHtmlParser teamMatchesParser) {
        this.restClient = restClient;
        this.parser = parser;
        this.teamMatchesParser = teamMatchesParser;
    }

    @Override
    public List<FlaStandingEntryResponse> getStandings(Long championnatId, Long saisonId) {
        log.info("Fetching FLA standings, championnatId={}, saisonId={}", championnatId, saisonId);

        String html = getHtml("/championships/{championshipId}/rankings?season={seasonId}", championnatId, saisonId);
        List<FlaStandingEntryResponse> standings = parser.parse(html, championnatId);
        log.info("Fetched FLA standings, championnatId={}, saisonId={}, teamCount={}",
                championnatId, saisonId, standings.size());
        return standings;
    }

    @Override
    public List<FlaTeamMatchResponse> getTeamMatches(Long flaTeamId) {
        if (flaTeamId == null || flaTeamId <= 0) {
            throw new IllegalArgumentException("flaTeamId must be positive");
        }
        log.info("Fetching FLA team matches, flaTeamId={}", flaTeamId);
        return getTeamMatchesPage(flaTeamId).matches();
    }

    @Override
    public FlaTeamMatchesPageResponse getTeamMatchesPage(Long flaTeamId) {
        if (flaTeamId == null || flaTeamId <= 0) {
            throw new IllegalArgumentException("flaTeamId must be positive");
        }
        return teamMatchesParser.parsePage(getHtml("/teams/{teamId}", flaTeamId), flaTeamId);
    }

    @Override
    public List<FlaTeamMatchResponse> getRecentCompletedMatches(Long flaTeamId, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        LocalDate today = LocalDate.now(ZoneId.of("Europe/Paris"));
        return getTeamMatches(flaTeamId).stream()
                .filter(FlaTeamMatchResponse::hasCompletedScore)
                .filter(match -> match.matchDate() != null && !match.matchDate().isAfter(today))
                .distinct()
                .sorted(Comparator.comparing(FlaTeamMatchResponse::matchDate).reversed()
                        .thenComparing(FlaTeamMatchResponse::kickoffTime,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limit)
                .toList();
    }

    private String getHtml(String uri, Object... variables) {
        try {
            return restClient.get()
                    .uri(uri, variables)
                    .accept(MediaType.TEXT_HTML)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful() && !status.isError(), (request, response) -> {
                        throw new FlaClientException(HttpStatus.BAD_GATEWAY, "FLA_HTTP_ERROR",
                                "FLA service returned an unexpected HTTP status");
                    })
                    .body(String.class);
        } catch (FlaClientException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw mapStatusException(exception, uri.startsWith("/teams/") ? "team matches" : "standings");
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
                    "FLA response could not be parsed"
            );
        }
    }

    // HTTP exceptions can embed response bodies; do not retain them in logged exception chains.
    private FlaClientException mapStatusException(RestClientResponseException exception, String resource) {
        HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
        if (status != null && status.is4xxClientError()) {
            return new FlaClientException(
                    HttpStatus.BAD_GATEWAY,
                    "FLA_INVALID_REQUEST",
                    "FLA rejected " + resource + " request"
            );
        }
        if (status != null && status.is5xxServerError()) {
            return new FlaClientException(
                    HttpStatus.BAD_GATEWAY,
                    "FLA_UNAVAILABLE",
                    "FLA service is unavailable"
            );
        }
        return new FlaClientException(
                HttpStatus.BAD_GATEWAY,
                "FLA_HTTP_ERROR",
                "FLA service returned an unexpected HTTP status"
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
