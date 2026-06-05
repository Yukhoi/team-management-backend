package com.yukai.team.teamservice.controller;

import com.yukai.team.teamservice.dto.player.ValidatePlayersRequest;
import com.yukai.team.teamservice.dto.player.ValidatePlayersResponse;
import com.yukai.team.teamservice.entity.Player;
import com.yukai.team.teamservice.repository.PlayerRepository;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/players")
@Hidden
public class InternalPlayerController {

    private final PlayerRepository playerRepository;

    public InternalPlayerController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @PostMapping("/validate")
    public ValidatePlayersResponse validatePlayers(@Valid @RequestBody ValidatePlayersRequest request) {
        Set<Long> requestedPlayerIds = new HashSet<>(request.getPlayerIds());
        Set<Long> validPlayerIds = playerRepository.findAllById(requestedPlayerIds)
                .stream()
                .map(Player::getId)
                .collect(Collectors.toSet());

        Set<Long> invalidPlayerIds = new HashSet<>(requestedPlayerIds);
        invalidPlayerIds.removeAll(validPlayerIds);

        return new ValidatePlayersResponse(validPlayerIds, invalidPlayerIds);
    }
}
