package com.yukai.team.teamservice.internal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidateMatchTeamsResponse {

    private Boolean valid;
    private InternalTeamInfo ourTeam;
    private InternalTeamInfo opponentTeam;
}
