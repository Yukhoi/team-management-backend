package com.yukai.team.teamservice.outbox;

public record EventOperator(
        Long userId,
        String username
) {
}
