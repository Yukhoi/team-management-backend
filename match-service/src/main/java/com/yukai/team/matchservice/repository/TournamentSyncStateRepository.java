package com.yukai.team.matchservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TournamentSyncStateRepository {
    private final JdbcTemplate jdbc;

    /** INSERT handles concurrent first use; the row lock is held until the caller's transaction commits. */
    public State lock(Long tournamentId) {
        jdbc.update("INSERT INTO match.tournament_sync_state (tournament_id, last_applied_version, updated_at) "
                + "VALUES (?, -1, CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING", tournamentId);
        return jdbc.queryForObject("SELECT last_applied_version, name, season FROM match.tournament_sync_state "
                + "WHERE tournament_id = ? FOR UPDATE", (rs, n) -> new State(rs.getLong(1), rs.getString(2), rs.getString(3)), tournamentId);
    }

    public void save(Long id, String name, String season, Long version) {
        jdbc.update("UPDATE match.tournament_sync_state SET last_applied_version=?, name=?, season=?, updated_at=CURRENT_TIMESTAMP "
                + "WHERE tournament_id=?", version, name, season, id);
    }

    public record State(long version, String name, String season) { }
}
