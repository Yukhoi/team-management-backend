package com.yukai.team.teamservice.dto.player;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class ValidatePlayersResponse {

    private Set<Long> validPlayerIds;
    private Set<Long> invalidPlayerIds;
}
