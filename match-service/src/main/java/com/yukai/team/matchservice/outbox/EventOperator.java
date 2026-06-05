package com.yukai.team.matchservice.outbox;

public record EventOperator(
        Long userId,
        String username
) {
}
