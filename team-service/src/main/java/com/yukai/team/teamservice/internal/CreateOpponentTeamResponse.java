package com.yukai.team.teamservice.internal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateOpponentTeamResponse {

    private Long teamId;
    private String teamName;
}
