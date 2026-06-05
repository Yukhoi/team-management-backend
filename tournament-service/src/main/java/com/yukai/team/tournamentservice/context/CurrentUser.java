package com.yukai.team.tournamentservice.context;

import java.util.List;

public record CurrentUser(
        Long userId,
        String username,
        List<String> roles
) {
}
