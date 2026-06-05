package com.yukai.team.matchservice.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.matchservice.client.dto.CreateOpponentTeamRequest;
import com.yukai.team.matchservice.client.dto.CreateOpponentTeamResponse;
import com.yukai.team.matchservice.client.dto.ValidateMatchTeamsRequest;
import com.yukai.team.matchservice.client.dto.ValidateMatchTeamsResponse;
import com.yukai.team.matchservice.client.dto.ValidatePlayersRequest;
import com.yukai.team.matchservice.client.dto.ValidatePlayersResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Set;

@Component
public class TeamServiceClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public TeamServiceClient(
            RestClient.Builder builder,
            @Value("${services.team-service.base-url}") String baseUrl,
            ObjectMapper objectMapper
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public ValidatePlayersResponse validatePlayers(Set<Long> playerIds) {
        return restClient.post()
                .uri("/internal/players/validate")
                .body(new ValidatePlayersRequest(playerIds))
                .retrieve()
                .body(ValidatePlayersResponse.class);
    }

    public ValidateMatchTeamsResponse validateMatchTeams(Long ourTeamId, Long opponentTeamId) {
        try {
            return restClient.post()
                    .uri("/internal/teams/validate-match-teams")
                    .body(new ValidateMatchTeamsRequest(ourTeamId, opponentTeamId))
                    .retrieve()
                    .body(ValidateMatchTeamsResponse.class);
        } catch (HttpClientErrorException ex) {
            throw new IllegalArgumentException(resolveTeamServiceErrorMessage(ex), ex);
        }
    }

    public CreateOpponentTeamResponse createOpponentTeam(String name) {
        try {
            return restClient.post()
                    .uri("/internal/teams/opponents")
                    .body(new CreateOpponentTeamRequest(name))
                    .retrieve()
                    .body(CreateOpponentTeamResponse.class);
        } catch (HttpClientErrorException ex) {
            throw new IllegalArgumentException(resolveTeamServiceErrorMessage(ex), ex);
        }
    }

    private String resolveTeamServiceErrorMessage(HttpClientErrorException ex) {
        try {
            String message = objectMapper.readTree(ex.getResponseBodyAsString())
                    .path("message")
                    .asText(null);
            if (message != null && !message.isBlank()) {
                return message;
            }
        } catch (Exception ignored) {
        }
        return "Failed to call team-service";
    }
}
