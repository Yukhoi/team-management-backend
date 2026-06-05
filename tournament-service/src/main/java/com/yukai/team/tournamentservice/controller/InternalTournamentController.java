package com.yukai.team.tournamentservice.controller;

import com.yukai.team.tournamentservice.dto.response.TournamentSnapshotResponse;
import com.yukai.team.tournamentservice.service.TournamentService;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/tournaments")
@Hidden
public class InternalTournamentController {

    private final TournamentService tournamentService;

    public InternalTournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @GetMapping("/{id}/snapshot")
    public TournamentSnapshotResponse getTournamentSnapshot(@PathVariable Long id) {
        return tournamentService.getTournamentSnapshot(id);
    }
}
