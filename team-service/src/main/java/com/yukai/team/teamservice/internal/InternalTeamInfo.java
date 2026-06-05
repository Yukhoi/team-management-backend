package com.yukai.team.teamservice.internal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InternalTeamInfo {

    private Long id;
    private String name;
    private Boolean isOurTeam;
}
