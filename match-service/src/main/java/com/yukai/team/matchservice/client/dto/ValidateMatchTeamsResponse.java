package com.yukai.team.matchservice.client.dto;

import lombok.Data;

@Data
public class ValidateMatchTeamsResponse {

    private Boolean valid;
    private InternalTeamInfo ourTeam;
    private InternalTeamInfo opponentTeam;
}
