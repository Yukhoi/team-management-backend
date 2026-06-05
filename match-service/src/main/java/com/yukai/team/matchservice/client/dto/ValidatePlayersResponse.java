package com.yukai.team.matchservice.client.dto;

import lombok.Data;

import java.util.Set;

@Data
public class ValidatePlayersResponse {

    private Set<Long> validPlayerIds;
    private Set<Long> invalidPlayerIds;
}
