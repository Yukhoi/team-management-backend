package com.yukai.team.teamservice.service;

import com.yukai.team.teamservice.dto.player.ChangePlayerStatusRequest;
import com.yukai.team.teamservice.dto.player.CreatePlayerRequest;
import com.yukai.team.teamservice.dto.player.PlayerResponse;
import com.yukai.team.teamservice.dto.player.UpdatePlayerRequest;
import com.yukai.team.teamservice.entity.Player;
import com.yukai.team.teamservice.entity.PlayerCurrentStatus;
import com.yukai.team.teamservice.entity.PlayerPosition;
import com.yukai.team.teamservice.entity.PlayerRegistrationStatus;
import com.yukai.team.teamservice.entity.PlayerStatusHistory;
import com.yukai.team.teamservice.entity.Team;
import com.yukai.team.teamservice.exception.BusinessException;
import com.yukai.team.teamservice.exception.ResourceNotFoundException;
import com.yukai.team.teamservice.repository.PlayerRepository;
import com.yukai.team.teamservice.repository.PlayerStatusHistoryRepository;
import com.yukai.team.teamservice.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class PlayerServiceTest {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerStatusHistoryRepository playerStatusHistoryRepository;

    @Test
    void should_create_player_successfully() {
        Team team = createTeam("Team A");
        CreatePlayerRequest request = buildCreateRequest(team.getId(), "Alice", 10);

        PlayerResponse response = playerService.createPlayer(request);

        assertNotNull(response.getId());
        assertEquals(team.getId(), response.getTeamId());
        assertEquals("Alice", response.getName());
        assertEquals(10, response.getJerseyNumber());
        assertEquals(PlayerPosition.FORWARD, response.getPosition());
        assertEquals(PlayerRegistrationStatus.REGISTERED, response.getRegistrationStatus());
        assertEquals(PlayerCurrentStatus.ACTIVE, response.getCurrentStatus());
        assertTrue(playerRepository.findByIdAndDeletedFlagFalse(response.getId()).isPresent());
    }

    @Test
    void should_throw_when_create_player_team_not_found() {
        CreatePlayerRequest request = buildCreateRequest(999999L, "Ghost", 9);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> playerService.createPlayer(request));

        assertEquals("Team not found: 999999", ex.getMessage());
    }

    @Test
    void should_throw_when_jersey_number_conflicts_in_same_team() {
        Team team = createTeam("Team A");
        playerService.createPlayer(buildCreateRequest(team.getId(), "Alice", 7));

        CreatePlayerRequest duplicateRequest = buildCreateRequest(team.getId(), "Bob", 7);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> playerService.createPlayer(duplicateRequest));

        assertEquals("Jersey number already exists in team: 7", ex.getMessage());
    }

    @Test
    void should_allow_same_jersey_number_in_different_teams() {
        Team teamA = createTeam("Team A");
        Team teamB = createTeam("Team B");

        PlayerResponse firstPlayer = playerService.createPlayer(buildCreateRequest(teamA.getId(), "Alice", 9));
        PlayerResponse secondPlayer = playerService.createPlayer(buildCreateRequest(teamB.getId(), "Bob", 9));

        assertNotNull(firstPlayer.getId());
        assertNotNull(secondPlayer.getId());
        assertEquals(9, firstPlayer.getJerseyNumber());
        assertEquals(9, secondPlayer.getJerseyNumber());
        assertEquals(teamA.getId(), firstPlayer.getTeamId());
        assertEquals(teamB.getId(), secondPlayer.getTeamId());
    }

    @Test
    void should_allow_reuse_jersey_number_when_existing_player_left() {
        Team team = createTeam("Team A");
        Player player = createPlayerEntity(team, "Alice", 12, PlayerCurrentStatus.ACTIVE);

        ChangePlayerStatusRequest leaveRequest = new ChangePlayerStatusRequest();
        leaveRequest.setNewStatus(PlayerCurrentStatus.LEFT);
        leaveRequest.setChangedBy(2001L);
        leaveRequest.setRemark("left the team");
        playerService.changePlayerStatus(player.getId(), leaveRequest);

        PlayerResponse replacementPlayer = playerService.createPlayer(buildCreateRequest(team.getId(), "Bob", 12));

        assertNotNull(replacementPlayer.getId());
        assertEquals(12, replacementPlayer.getJerseyNumber());
        assertEquals(team.getId(), replacementPlayer.getTeamId());
        assertEquals("Bob", replacementPlayer.getName());
    }

    @Test
    void should_change_status_to_injured_and_write_history() {
        Team team = createTeam("Team A");
        Player player = createPlayerEntity(team, "Alice", 8, PlayerCurrentStatus.ACTIVE);
        ChangePlayerStatusRequest request = new ChangePlayerStatusRequest();
        request.setNewStatus(PlayerCurrentStatus.INJURED);
        request.setChangedBy(1001L);
        request.setRemark("ankle injury");

        PlayerResponse response = playerService.changePlayerStatus(player.getId(), request);

        assertEquals(PlayerCurrentStatus.INJURED, response.getCurrentStatus());
        List<PlayerStatusHistory> histories = playerStatusHistoryRepository.findByPlayerIdOrderByChangedAtAsc(player.getId());
        assertEquals(1, histories.size());
        PlayerStatusHistory history = histories.get(0);
        assertEquals(PlayerCurrentStatus.ACTIVE, history.getOldStatus());
        assertEquals(PlayerCurrentStatus.INJURED, history.getNewStatus());
        assertEquals(1001L, history.getChangedBy());
        assertEquals("ankle injury", history.getRemark());
    }

    @Test
    void should_not_write_history_when_status_not_changed() {
        Team team = createTeam("Team A");
        Player player = createPlayerEntity(team, "Alice", 18, PlayerCurrentStatus.ACTIVE);
        ChangePlayerStatusRequest request = new ChangePlayerStatusRequest();
        request.setNewStatus(PlayerCurrentStatus.ACTIVE);
        request.setChangedBy(1003L);
        request.setRemark("no actual change");

        PlayerResponse response = playerService.changePlayerStatus(player.getId(), request);

        assertEquals(PlayerCurrentStatus.ACTIVE, response.getCurrentStatus());
        List<PlayerStatusHistory> histories = playerStatusHistoryRepository.findByPlayerIdOrderByChangedAtAsc(player.getId());
        assertTrue(histories.isEmpty());
    }

    @Test
    void should_set_left_date_when_status_changes_to_left() {
        Team team = createTeam("Team A");
        Player player = createPlayerEntity(team, "Alice", 11, PlayerCurrentStatus.ACTIVE);
        ChangePlayerStatusRequest request = new ChangePlayerStatusRequest();
        request.setNewStatus(PlayerCurrentStatus.LEFT);
        request.setChangedBy(1002L);
        request.setRemark("left the team");

        PlayerResponse response = playerService.changePlayerStatus(player.getId(), request);

        assertEquals(PlayerCurrentStatus.LEFT, response.getCurrentStatus());
        assertNotNull(response.getLeftDate());
        assertEquals(LocalDate.now(), response.getLeftDate());
    }

    @Test
    void should_update_player_successfully() {
        Team team = createTeam("Team A");
        Player player = createPlayerEntity(team, "Alice", 15, PlayerCurrentStatus.ACTIVE);
        UpdatePlayerRequest request = new UpdatePlayerRequest();
        request.setName("Alice Updated");
        request.setJerseyNumber(16);
        request.setBirthDate(LocalDate.of(1999, 2, 2));
        request.setPhone("13900139000");
        request.setPosition(PlayerPosition.MIDFIELDER);
        request.setRegistrationStatus(PlayerRegistrationStatus.UNREGISTERED);
        request.setJoinedDate(LocalDate.of(2025, 1, 1));
        request.setLeftDate(null);
        request.setRemark("updated player");

        PlayerResponse response = playerService.updatePlayer(player.getId(), request);

        assertEquals(player.getId(), response.getId());
        assertEquals("Alice Updated", response.getName());
        assertEquals(16, response.getJerseyNumber());
        assertEquals(LocalDate.of(1999, 2, 2), response.getBirthDate());
        assertEquals("13900139000", response.getPhone());
        assertEquals(PlayerPosition.MIDFIELDER, response.getPosition());
        assertEquals(PlayerRegistrationStatus.UNREGISTERED, response.getRegistrationStatus());
        assertEquals(LocalDate.of(2025, 1, 1), response.getJoinedDate());
        assertEquals("updated player", response.getRemark());
    }

    @Test
    void should_not_find_player_after_soft_delete() {
        Team team = createTeam("Team A");
        Player player = createPlayerEntity(team, "Alice", 15, PlayerCurrentStatus.ACTIVE);

        playerService.deletePlayer(player.getId());

        assertFalse(playerRepository.findByIdAndDeletedFlagFalse(player.getId()).isPresent());
        assertThrows(ResourceNotFoundException.class, () -> playerService.getPlayerById(player.getId()));
    }

    private Team createTeam(String name) {
        Team team = new Team();
        team.setName(name);
        team.setShortName(name.substring(0, Math.min(name.length(), 10)));
        team.setDescription(name + " description");
        team.setIsOurTeam(Boolean.TRUE);
        team.setRemark("test team");
        return teamRepository.save(team);
    }

    private Player createPlayerEntity(Team team, String name, Integer jerseyNumber, PlayerCurrentStatus currentStatus) {
        Player player = new Player();
        player.setTeam(team);
        player.setName(name);
        player.setJerseyNumber(jerseyNumber);
        player.setBirthDate(LocalDate.of(2000, 1, 1));
        player.setPhone("13800138000");
        player.setPosition(PlayerPosition.FORWARD);
        player.setRegistrationStatus(PlayerRegistrationStatus.REGISTERED);
        player.setCurrentStatus(currentStatus);
        player.setJoinedDate(LocalDate.of(2024, 1, 1));
        player.setRemark("test player");
        player.setDeletedFlag(Boolean.FALSE);
        return playerRepository.save(player);
    }

    private CreatePlayerRequest buildCreateRequest(Long teamId, String name, Integer jerseyNumber) {
        CreatePlayerRequest request = new CreatePlayerRequest();
        request.setTeamId(teamId);
        request.setName(name);
        request.setJerseyNumber(jerseyNumber);
        request.setBirthDate(LocalDate.of(2000, 1, 1));
        request.setPhone("13800138000");
        request.setPosition(PlayerPosition.FORWARD);
        request.setRegistrationStatus(PlayerRegistrationStatus.REGISTERED);
        request.setCurrentStatus(PlayerCurrentStatus.ACTIVE);
        request.setJoinedDate(LocalDate.of(2024, 1, 1));
        request.setRemark("test player");
        return request;
    }
}
