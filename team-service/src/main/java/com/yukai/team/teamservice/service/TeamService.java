package com.yukai.team.teamservice.service;

import com.yukai.team.teamservice.dto.common.PageResponse;
import com.yukai.team.teamservice.dto.team.CreateTeamRequest;
import com.yukai.team.teamservice.dto.team.TeamResponse;
import com.yukai.team.teamservice.dto.team.UpdateTeamRequest;

public interface TeamService {

    TeamResponse createTeam(CreateTeamRequest request);

    TeamResponse getTeamById(Long id);

    PageResponse<TeamResponse> listTeams(int page, int size);

    TeamResponse getOurTeam();

    TeamResponse updateTeam(Long id, UpdateTeamRequest request);

    void deleteTeam(Long id);
}
