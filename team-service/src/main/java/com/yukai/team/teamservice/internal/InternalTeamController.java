package com.yukai.team.teamservice.internal;

import com.yukai.team.teamservice.entity.Team;
import com.yukai.team.teamservice.exception.BusinessException;
import com.yukai.team.teamservice.repository.TeamRepository;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/teams")
@Hidden
public class InternalTeamController {

    private final TeamRepository teamRepository;

    public InternalTeamController(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @PostMapping("/validate-match-teams")
    public ValidateMatchTeamsResponse validateMatchTeams(@Valid @RequestBody ValidateMatchTeamsRequest request) {
        Team ourTeam = teamRepository.findById(request.getOurTeamId())
                .orElseThrow(() -> new BusinessException("ourTeamId not found"));
        if (!Boolean.TRUE.equals(ourTeam.getIsOurTeam())) {
            throw new BusinessException("ourTeamId must be our team");
        }

        Team opponentTeam = null;
        if (request.getOpponentTeamId() != null) {
            if (request.getOurTeamId().equals(request.getOpponentTeamId())) {
                throw new BusinessException("opponentTeamId cannot equal ourTeamId");
            }
            opponentTeam = teamRepository.findById(request.getOpponentTeamId())
                    .orElseThrow(() -> new BusinessException("opponentTeamId not found"));
            if (Boolean.TRUE.equals(opponentTeam.getIsOurTeam())) {
                throw new BusinessException("opponentTeamId cannot be our team");
            }
        }

        return new ValidateMatchTeamsResponse(
                true,
                toInternalTeamInfo(ourTeam),
                opponentTeam == null ? null : toInternalTeamInfo(opponentTeam)
        );
    }

    @PostMapping("/opponents")
    public CreateOpponentTeamResponse createOpponentTeam(@Valid @RequestBody CreateOpponentTeamRequest request) {
        String normalizedName = request.getName().trim();
        Team team = teamRepository.findByNameAndIsOurTeamFalse(normalizedName)
                .orElseGet(() -> createOpponent(normalizedName));
        return new CreateOpponentTeamResponse(team.getId(), team.getName());
    }

    private Team createOpponent(String name) {
        Team team = new Team();
        team.setName(name);
        team.setIsOurTeam(false);
        return teamRepository.save(team);
    }

    private InternalTeamInfo toInternalTeamInfo(Team team) {
        return new InternalTeamInfo(team.getId(), team.getName(), team.getIsOurTeam());
    }
}
