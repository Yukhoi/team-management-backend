package com.yukai.team.teamservice.service;

import com.yukai.team.teamservice.dto.common.PageResponse;
import com.yukai.team.teamservice.dto.player.ChangePlayerStatusRequest;
import com.yukai.team.teamservice.dto.player.CreatePlayerRequest;
import com.yukai.team.teamservice.dto.player.PlayerResponse;
import com.yukai.team.teamservice.dto.player.UpdatePlayerRequest;

public interface PlayerService {

    PlayerResponse createPlayer(CreatePlayerRequest request);

    PlayerResponse getPlayerById(Long id);

    PageResponse<PlayerResponse> listPlayers(int page, int size);

    PageResponse<PlayerResponse> listPlayersByTeamId(Long teamId, int page, int size);

    PlayerResponse updatePlayer(Long id, UpdatePlayerRequest request);

    void deletePlayer(Long id);

    PlayerResponse changePlayerStatus(Long id, ChangePlayerStatusRequest request);
}
