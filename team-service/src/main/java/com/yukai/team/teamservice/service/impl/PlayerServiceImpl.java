package com.yukai.team.teamservice.service.impl;

import com.yukai.team.teamservice.dto.common.PageResponse;
import com.yukai.team.teamservice.dto.player.ChangePlayerStatusRequest;
import com.yukai.team.teamservice.dto.player.CreatePlayerRequest;
import com.yukai.team.teamservice.dto.player.PlayerResponse;
import com.yukai.team.teamservice.dto.player.UpdatePlayerRequest;
import com.yukai.team.teamservice.entity.Player;
import com.yukai.team.teamservice.entity.PlayerCurrentStatus;
import com.yukai.team.teamservice.entity.PlayerRegistrationStatus;
import com.yukai.team.teamservice.entity.PlayerStatusHistory;
import com.yukai.team.teamservice.entity.Team;
import com.yukai.team.teamservice.exception.BusinessException;
import com.yukai.team.teamservice.exception.ResourceNotFoundException;
import com.yukai.team.teamservice.repository.PlayerRepository;
import com.yukai.team.teamservice.repository.PlayerStatusHistoryRepository;
import com.yukai.team.teamservice.repository.TeamRepository;
import com.yukai.team.teamservice.service.OutboxEventService;
import com.yukai.team.teamservice.service.PlayerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final PlayerStatusHistoryRepository playerStatusHistoryRepository;
    private final OutboxEventService outboxEventService;

    public PlayerServiceImpl(
            PlayerRepository playerRepository,
            TeamRepository teamRepository,
            PlayerStatusHistoryRepository playerStatusHistoryRepository,
            OutboxEventService outboxEventService
    ) {
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.playerStatusHistoryRepository = playerStatusHistoryRepository;
        this.outboxEventService = outboxEventService;
    }

    @Override
    @Transactional
    public PlayerResponse createPlayer(CreatePlayerRequest request) {
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + request.getTeamId()));

        validateJerseyNumber(team.getId(), request.getJerseyNumber());

        Player player = new Player();
        player.setTeam(team);
        player.setName(request.getName().trim());
        player.setJerseyNumber(request.getJerseyNumber());
        player.setBirthDate(request.getBirthDate());
        player.setPhone(request.getPhone());
        player.setPosition(request.getPosition());
        player.setRegistrationStatus(defaultRegistrationStatus(request.getRegistrationStatus()));
        player.setCurrentStatus(defaultCurrentStatus(request.getCurrentStatus()));
        player.setJoinedDate(request.getJoinedDate());
        player.setRemark(request.getRemark());
        player.setDeletedFlag(Boolean.FALSE);

        Player savedPlayer = playerRepository.save(player);
        outboxEventService.saveEvent("player", savedPlayer.getId(), "player.created", buildPlayerPayload(savedPlayer));
        return toResponse(savedPlayer);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerResponse getPlayerById(Long id) {
        return toResponse(getActivePlayer(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PlayerResponse> listPlayers(int page, int size) {
        Pageable pageable = buildPageable(page, size);
        Page<Player> playerPage = playerRepository.findByDeletedFlagFalse(pageable);
        List<PlayerResponse> content = playerPage.getContent().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.from(playerPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PlayerResponse> listPlayersByTeamId(Long teamId, int page, int size) {
        Pageable pageable = buildPageable(page, size);
        Page<Player> playerPage = playerRepository.findByTeamIdAndDeletedFlagFalse(teamId, pageable);
        List<PlayerResponse> content = playerPage.getContent().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.from(playerPage, content);
    }

    @Override
    @Transactional
    public PlayerResponse updatePlayer(Long id, UpdatePlayerRequest request) {
        Player player = getActivePlayer(id);

        if (!Objects.equals(player.getJerseyNumber(), request.getJerseyNumber())) {
            validateJerseyNumber(player.getTeam().getId(), request.getJerseyNumber());
        }

        player.setName(request.getName().trim());
        player.setJerseyNumber(request.getJerseyNumber());
        player.setBirthDate(request.getBirthDate());
        player.setPhone(request.getPhone());
        player.setPosition(request.getPosition());
        player.setRegistrationStatus(request.getRegistrationStatus());
        player.setJoinedDate(request.getJoinedDate());
        player.setLeftDate(request.getLeftDate());
        player.setRemark(request.getRemark());

        Player savedPlayer = playerRepository.save(player);
        outboxEventService.saveEvent("player", savedPlayer.getId(), "player.updated", buildPlayerPayload(savedPlayer));
        return toResponse(savedPlayer);
    }

    @Override
    @Transactional
    public void deletePlayer(Long id) {
        Player player = getActivePlayer(id);
        player.setDeletedFlag(Boolean.TRUE);
        Player savedPlayer = playerRepository.save(player);
        outboxEventService.saveEvent("player", savedPlayer.getId(), "player.deleted", buildPlayerPayload(savedPlayer));
    }

    @Override
    @Transactional
    public PlayerResponse changePlayerStatus(Long id, ChangePlayerStatusRequest request) {
        Player player = getActivePlayer(id);

        PlayerCurrentStatus oldStatus = player.getCurrentStatus();
        PlayerCurrentStatus newStatus = request.getNewStatus();

        if (oldStatus == newStatus) {
            return toResponse(player);
        }

        player.setCurrentStatus(newStatus);
        if (newStatus == PlayerCurrentStatus.LEFT) {
            player.setLeftDate(LocalDate.now());
        }

        PlayerStatusHistory history = new PlayerStatusHistory();
        history.setPlayer(player);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(request.getChangedBy());
        history.setRemark(request.getRemark());

        Player savedPlayer = playerRepository.save(player);
        playerStatusHistoryRepository.save(history);
        outboxEventService.saveEvent(
                "player",
                savedPlayer.getId(),
                "player.status.changed",
                buildPlayerStatusChangedPayload(savedPlayer, oldStatus, newStatus)
        );
        return toResponse(savedPlayer);
    }

    private Map<String, Object> buildPlayerPayload(Player player) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerId", player.getId());
        payload.put("teamId", player.getTeam().getId());
        payload.put("name", player.getName());
        payload.put("currentStatus", player.getCurrentStatus());
        return payload;
    }

    private Map<String, Object> buildPlayerStatusChangedPayload(
            Player player,
            PlayerCurrentStatus oldStatus,
            PlayerCurrentStatus newStatus
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("playerId", player.getId());
        payload.put("teamId", player.getTeam().getId());
        payload.put("oldStatus", oldStatus);
        payload.put("newStatus", newStatus);
        return payload;
    }

    private Player getActivePlayer(Long id) {
        return playerRepository.findByIdAndDeletedFlagFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found: " + id));
    }

    private void validateJerseyNumber(Long teamId, Integer jerseyNumber) {
        if (jerseyNumber == null) {
            return;
        }
        boolean exists = playerRepository.existsByTeamIdAndJerseyNumberAndDeletedFlagFalseAndCurrentStatusNot(
                teamId,
                jerseyNumber,
                PlayerCurrentStatus.LEFT
        );
        if (exists) {
            throw new BusinessException("Jersey number already exists in team: " + jerseyNumber);
        }
    }

    private PlayerRegistrationStatus defaultRegistrationStatus(PlayerRegistrationStatus status) {
        return status == null ? PlayerRegistrationStatus.REGISTERED : status;
    }

    private PlayerCurrentStatus defaultCurrentStatus(PlayerCurrentStatus status) {
        return status == null ? PlayerCurrentStatus.ACTIVE : status;
    }

    private Pageable buildPageable(int page, int size) {
        return PageRequest.of(page, size, Sort.by("id").ascending());
    }

    private PlayerResponse toResponse(Player player) {
        PlayerResponse response = new PlayerResponse();
        response.setId(player.getId());
        response.setTeamId(player.getTeam().getId());
        response.setTeamName(player.getTeam().getName());
        response.setName(player.getName());
        response.setJerseyNumber(player.getJerseyNumber());
        response.setBirthDate(player.getBirthDate());
        response.setPhone(player.getPhone());
        response.setPosition(player.getPosition());
        response.setRegistrationStatus(player.getRegistrationStatus());
        response.setCurrentStatus(player.getCurrentStatus());
        response.setJoinedDate(player.getJoinedDate());
        response.setLeftDate(player.getLeftDate());
        response.setRemark(player.getRemark());
        response.setCreatedAt(player.getCreatedAt());
        response.setUpdatedAt(player.getUpdatedAt());
        response.setVersion(player.getVersion());
        return response;
    }
}
