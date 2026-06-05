package com.yukai.team.matchservice.context;

import java.util.List;

public record CurrentUser(
        Long userId,
        String username,
        List<String> roles
) {
}
