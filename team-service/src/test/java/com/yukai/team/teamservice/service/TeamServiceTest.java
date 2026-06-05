package com.yukai.team.teamservice.service;

import com.yukai.team.teamservice.dto.team.CreateTeamRequest;
import com.yukai.team.teamservice.dto.team.TeamResponse;
import com.yukai.team.teamservice.dto.team.UpdateTeamRequest;
import com.yukai.team.teamservice.entity.Player;
import com.yukai.team.teamservice.entity.PlayerCurrentStatus;
import com.yukai.team.teamservice.entity.PlayerPosition;
import com.yukai.team.teamservice.entity.PlayerRegistrationStatus;
import com.yukai.team.teamservice.entity.Team;
import com.yukai.team.teamservice.exception.BusinessException;
import com.yukai.team.teamservice.exception.ResourceNotFoundException;
import com.yukai.team.teamservice.repository.PlayerRepository;
import com.yukai.team.teamservice.repository.TeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class TeamServiceTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Test
    void should_create_team_successfully() {
        CreateTeamRequest request = buildCreateTeamRequest("Team A", null);

        TeamResponse response = teamService.createTeam(request);

        assertNotNull(response.getId());
        assertEquals("Team A", response.getName());
        assertFalse(response.getIsOurTeam());
        Optional<Team> savedTeam = teamRepository.findById(response.getId());
        assertTrue(savedTeam.isPresent());
        assertEquals("Team A", savedTeam.get().getName());
        assertFalse(savedTeam.get().getIsOurTeam());
    }

    @Test
    void should_throw_when_create_team_with_duplicate_name() {
        teamService.createTeam(buildCreateTeamRequest("Team A", Boolean.FALSE));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teamService.createTeam(buildCreateTeamRequest("Team A", Boolean.FALSE)));

        assertEquals("Team name already exists: Team A", ex.getMessage());
    }

    @Test
    void should_throw_when_create_second_our_team() {
        teamService.createTeam(buildCreateTeamRequest("Team A", Boolean.TRUE));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teamService.createTeam(buildCreateTeamRequest("Team B", Boolean.TRUE)));

        assertEquals("Only one our team is allowed", ex.getMessage());
    }

    @Test
    void should_get_team_by_id_successfully() {
        Team team = createTeam("Team A", Boolean.FALSE);

        TeamResponse response = teamService.getTeamById(team.getId());

        assertEquals(team.getId(), response.getId());
        assertEquals("Team A", response.getName());
        assertEquals("Team A SN", response.getShortName());
        assertEquals("Team A description", response.getDescription());
        assertFalse(response.getIsOurTeam());
        assertEquals("Team A remark", response.getRemark());
    }

    @Test
    void should_throw_when_team_not_found() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> teamService.getTeamById(999999L));

        assertEquals("Team not found: 999999", ex.getMessage());
    }

    @Test
    void should_get_our_team_successfully() {
        createTeam("Team A", Boolean.FALSE);
        Team ourTeam = createTeam("Our Team", Boolean.TRUE);

        TeamResponse response = teamService.getOurTeam();

        assertEquals(ourTeam.getId(), response.getId());
        assertEquals("Our Team", response.getName());
        assertTrue(response.getIsOurTeam());
    }

    @Test
    void should_throw_when_our_team_not_found() {
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> teamService.getOurTeam());

        assertEquals("Our team not found", ex.getMessage());
    }

    @Test
    void should_update_team_successfully() {
        Team team = createTeam("Team A", Boolean.FALSE);
        UpdateTeamRequest request = new UpdateTeamRequest();
        request.setName("Team A Updated");
        request.setShortName("TAU");
        request.setDescription("updated description");
        request.setIsOurTeam(Boolean.TRUE);
        request.setRemark("updated remark");

        TeamResponse response = teamService.updateTeam(team.getId(), request);

        assertEquals(team.getId(), response.getId());
        assertEquals("Team A Updated", response.getName());
        assertEquals("TAU", response.getShortName());
        assertEquals("updated description", response.getDescription());
        assertTrue(response.getIsOurTeam());
        assertEquals("updated remark", response.getRemark());
    }

    @Test
    void should_keep_original_is_our_team_when_update_is_our_team_null() {
        Team team = createTeam("Team A", Boolean.TRUE);
        UpdateTeamRequest request = new UpdateTeamRequest();
        request.setName("Team A Updated");
        request.setShortName("TAU");
        request.setDescription("updated description");
        request.setIsOurTeam(null);
        request.setRemark("updated remark");

        TeamResponse response = teamService.updateTeam(team.getId(), request);

        assertEquals(team.getId(), response.getId());
        assertTrue(response.getIsOurTeam());
        assertEquals("Team A Updated", response.getName());
        assertEquals("TAU", response.getShortName());
    }

    @Test
    void should_throw_when_update_team_name_duplicate() {
        createTeam("Team A", Boolean.FALSE);
        Team teamB = createTeam("Team B", Boolean.FALSE);
        UpdateTeamRequest request = new UpdateTeamRequest();
        request.setName("Team A");
        request.setShortName("TB");
        request.setDescription("Team B description");
        request.setIsOurTeam(Boolean.FALSE);
        request.setRemark("Team B remark");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teamService.updateTeam(teamB.getId(), request));

        assertEquals("Team name already exists: Team A", ex.getMessage());
    }

    @Test
    void should_throw_when_update_to_second_our_team() {
        createTeam("Team A", Boolean.TRUE);
        Team teamB = createTeam("Team B", Boolean.FALSE);
        UpdateTeamRequest request = new UpdateTeamRequest();
        request.setName("Team B");
        request.setShortName("TB");
        request.setDescription("Team B description");
        request.setIsOurTeam(Boolean.TRUE);
        request.setRemark("Team B remark");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teamService.updateTeam(teamB.getId(), request));

        assertEquals("Only one our team is allowed", ex.getMessage());
    }

    @Test
    void should_delete_team_successfully_when_no_players() {
        Team team = createTeam("Team A", Boolean.FALSE);

        teamService.deleteTeam(team.getId());

        assertFalse(teamRepository.findById(team.getId()).isPresent());
    }

    @Test
    void should_throw_when_delete_team_with_players() {
        Team team = createTeam("Team A", Boolean.FALSE);
        createPlayer(team);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> teamService.deleteTeam(team.getId()));

        assertEquals("Please delete all players under this team first", ex.getMessage());
        assertTrue(teamRepository.findById(team.getId()).isPresent());
    }

    private Team createTeam(String name, Boolean isOurTeam) {
        Team team = new Team();
        team.setName(name);
        team.setShortName(name + " SN");
        team.setDescription(name + " description");
        team.setIsOurTeam(isOurTeam != null ? isOurTeam : Boolean.FALSE);
        team.setRemark(name + " remark");
        return teamRepository.save(team);
    }

    private Player createPlayer(Team team) {
        Player player = new Player();
        player.setTeam(team);
        player.setName(team.getName() + " Player");
        player.setJerseyNumber(10);
        player.setPosition(PlayerPosition.FORWARD);
        player.setRegistrationStatus(PlayerRegistrationStatus.REGISTERED);
        player.setCurrentStatus(PlayerCurrentStatus.ACTIVE);
        player.setDeletedFlag(Boolean.FALSE);
        return playerRepository.save(player);
    }

    private CreateTeamRequest buildCreateTeamRequest(String name, Boolean isOurTeam) {
        CreateTeamRequest request = new CreateTeamRequest();
        request.setName(name);
        request.setShortName(name + " SN");
        request.setDescription(name + " description");
        request.setIsOurTeam(isOurTeam);
        request.setRemark(name + " remark");
        return request;
    }
}
