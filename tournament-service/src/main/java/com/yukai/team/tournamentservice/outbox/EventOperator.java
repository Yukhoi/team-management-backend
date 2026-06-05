package com.yukai.team.tournamentservice.outbox;

public record EventOperator(
        Long userId,
        String username
) {
}
