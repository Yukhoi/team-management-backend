package com.yukai.team.matchservice.client.dto;

import lombok.Data;

@Data
public class InternalTeamInfo {

    private Long id;
    private String name;
    private Boolean isOurTeam;
}
