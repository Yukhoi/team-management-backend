package com.yukai.team.matchservice.client;

import com.yukai.team.matchservice.dto.response.TournamentSnapshotResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "tournament-service",
        url = "${services.tournament-service.base-url}"
)
public interface TournamentClient {

    @GetMapping("/internal/tournaments/{id}/snapshot")
    TournamentSnapshotResponse getTournamentSnapshot(@PathVariable("id") Long tournamentId);
}
