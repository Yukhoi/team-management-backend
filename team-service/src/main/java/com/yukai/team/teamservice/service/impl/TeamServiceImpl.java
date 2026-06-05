package com.yukai.team.teamservice.service.impl;

import com.yukai.team.teamservice.dto.common.PageResponse;
import com.yukai.team.teamservice.dto.team.CreateTeamRequest;
import com.yukai.team.teamservice.dto.team.TeamResponse;
import com.yukai.team.teamservice.dto.team.UpdateTeamRequest;
import com.yukai.team.teamservice.entity.Team;
import com.yukai.team.teamservice.exception.BusinessException;
import com.yukai.team.teamservice.exception.ResourceNotFoundException;
import com.yukai.team.teamservice.repository.PlayerRepository;
import com.yukai.team.teamservice.repository.TeamRepository;
import com.yukai.team.teamservice.service.OutboxEventService;
import com.yukai.team.teamservice.service.TeamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final OutboxEventService outboxEventService;

    public TeamServiceImpl(
            TeamRepository teamRepository,
            PlayerRepository playerRepository,
            OutboxEventService outboxEventService
    ) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.outboxEventService = outboxEventService;
    }

    @Override
    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request) {
        validateName(request.getName());

        String normalizedName = request.getName().trim();
        if (teamRepository.existsByName(normalizedName)) {
            throw new BusinessException("Team name already exists：" + normalizedName);
        }

        Boolean isOurTeam = request.getIsOurTeam() == null ? Boolean.FALSE : request.getIsOurTeam();
        validateOurTeamFlagForCreate(isOurTeam);

        Team team = new Team();
        team.setName(normalizedName);
        team.setShortName(trimToNull(request.getShortName()));
        team.setDescription(trimToNull(request.getDescription()));
        team.setIsOurTeam(isOurTeam);
        team.setRemark(trimToNull(request.getRemark()));

        Team savedTeam = teamRepository.save(team);
        outboxEventService.saveEvent("team", savedTeam.getId(), "team.created", buildTeamPayload(savedTeam));
        return toResponse(savedTeam);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse getTeamById(Long id) {
        return toResponse(getTeam(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TeamResponse> listTeams(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Team> teamPage = teamRepository.findAll(pageable);
        List<TeamResponse> content = teamPage.getContent().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.from(teamPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public TeamResponse getOurTeam() {
        Team team = teamRepository.findByIsOurTeamTrue()
                .orElseThrow(() -> new ResourceNotFoundException("Our team not found"));
        return toResponse(team);
    }

    @Override
    @Transactional
    public TeamResponse updateTeam(Long id, UpdateTeamRequest request) {
        Team team = getTeam(id);
        validateName(request.getName());

        String normalizedName = request.getName().trim();
        if (teamRepository.existsByNameAndIdNot(normalizedName, id)) {
            throw new BusinessException("球队名称已存在");
        }

        Boolean isOurTeam = request.getIsOurTeam() == null ? team.getIsOurTeam() : request.getIsOurTeam();
        validateOurTeamFlagForUpdate(isOurTeam, id);

        team.setName(normalizedName);
        team.setShortName(trimToNull(request.getShortName()));
        team.setDescription(trimToNull(request.getDescription()));
        team.setIsOurTeam(isOurTeam);
        team.setRemark(trimToNull(request.getRemark()));

        Team savedTeam = teamRepository.save(team);
        outboxEventService.saveEvent("team", savedTeam.getId(), "team.updated", buildTeamPayload(savedTeam));
        return toResponse(savedTeam);
    }

    @Override
    @Transactional
    public void deleteTeam(Long id) {
        Team team = getTeam(id);
        if (playerRepository.existsByTeamIdAndDeletedFlagFalse(id)) {
            throw new BusinessException("Please delete all players under this team first");
        }
        outboxEventService.saveEvent("team", team.getId(), "team.deleted", buildTeamPayload(team));
        teamRepository.delete(team);
    }

    private Map<String, Object> buildTeamPayload(Team team) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("teamId", team.getId());
        payload.put("name", team.getName());
        payload.put("isOurTeam", team.getIsOurTeam());
        return payload;
    }

    private Team getTeam(Long id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + id));
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new BusinessException("name must not be blank");
        }
    }

    private void validateOurTeamFlagForCreate(Boolean isOurTeam) {
        if (Boolean.TRUE.equals(isOurTeam) && teamRepository.existsByIsOurTeamTrue()) {
            throw new BusinessException("Only one our team is allowed");
        }
    }

    private void validateOurTeamFlagForUpdate(Boolean isOurTeam, Long id) {
        if (Boolean.TRUE.equals(isOurTeam) && teamRepository.existsByIsOurTeamTrueAndIdNot(id)) {
            throw new BusinessException("Only one our team is allowed");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private TeamResponse toResponse(Team team) {
        TeamResponse response = new TeamResponse();
        response.setId(team.getId());
        response.setName(team.getName());
        response.setShortName(team.getShortName());
        response.setDescription(team.getDescription());
        response.setIsOurTeam(team.getIsOurTeam());
        response.setRemark(team.getRemark());
        response.setCreatedAt(team.getCreatedAt());
        response.setUpdatedAt(team.getUpdatedAt());
        response.setVersion(team.getVersion());
        return response;
    }
}
