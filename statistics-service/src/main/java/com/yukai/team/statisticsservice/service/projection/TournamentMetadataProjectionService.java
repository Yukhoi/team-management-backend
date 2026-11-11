package com.yukai.team.statisticsservice.service.projection;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yukai.team.statisticsservice.dto.event.StatisticsEventEnvelope;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/** Serializes tournament metadata and match projection writes in this service's own database. */
@Service
@RequiredArgsConstructor
public class TournamentMetadataProjectionService {
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;
    private final LeaderboardProjectionService leaderboards;
    private final TeamRankingService teamRanking;

    @Transactional
    public void normalizeMatchEvent(StatisticsEventEnvelope event) {
        if (!(event.getData() instanceof ObjectNode data) || (!data.path("tournamentId").isIntegralNumber() || !data.path("tournamentId").canConvertToLong())) return;
        long id = data.path("tournamentId").asLong();
        State state = lock(id);
        if (state.version() >= 0) {
            data.put("tournamentNameSnapshot", state.name());
            data.put("seasonSnapshot", state.season());
        }
    }

    @Transactional
    public void apply(StatisticsEventEnvelope event) {
        var data = event.getData();
        if (!"tournament".equals(event.getAggregateType()) || data == null
                || (!data.path("tournamentId").isIntegralNumber() || !data.path("tournamentId").canConvertToLong()) || (!data.path("version").isIntegralNumber() || !data.path("version").canConvertToLong())
                || !data.path("name").isTextual() || !data.path("season").isTextual()) {
            throw new IllegalArgumentException("Invalid synchronized tournament metadata");
        }
        long id = data.path("tournamentId").asLong();
        long version = data.path("version").asLong();
        String name = data.path("name").asText();
        String season = data.path("season").asText();
        if (!Objects.equals(event.getAggregateId(), id) || id <= 0 || version < 0 || name.isBlank()
                || name.length() > 100 || season.isBlank() || season.length() > 30) {
            throw new IllegalArgumentException("Invalid synchronized tournament identity or metadata");
        }
        State previous = lock(id);
        if (version < previous.version()) return;
        if (version == previous.version() && (!name.equals(previous.name()) || !season.equals(previous.season()))) {
            throw new IllegalArgumentException("Conflicting synchronized tournament version");
        }
        entityManager.flush();
        entityManager.clear();
        // Existing stale matches can have split a tournament's totals into different season buckets.
        // Consolidate only duplicate buckets, preserving the sum of every stored counter.
        boolean playerMerged = collapse("player_stats_projection", "player_id", id,
                "appearances", "starts", "goals", "assists", "goal_involvements");
        boolean teamMerged = collapse("team_stats_projection", "team_id", id,
                "played", "win", "draw", "lose", "goals_for", "goals_against", "goal_diff", "points");
        boolean rebuildBoards = playerMerged || jdbc.queryForObject(
                "SELECT COUNT(*) FROM (SELECT board_type, entity_id FROM statistics.leaderboard_projection "
                        + "WHERE tournament_id=? GROUP BY board_type, entity_id HAVING COUNT(*)>1) duplicates", Integer.class, id) > 0;
        if (rebuildBoards) jdbc.update("DELETE FROM statistics.leaderboard_projection WHERE tournament_id=?", id);
        for (String table : new String[]{"match_summary_projection", "player_stats_projection", "team_stats_projection"}) {
            String seasonColumn = table.equals("match_summary_projection") ? "season_snapshot" : "season";
            jdbc.update("UPDATE statistics." + table + " SET tournament_name_snapshot=?, " + seasonColumn
                    + "=?, updated_at=CURRENT_TIMESTAMP WHERE tournament_id=? AND (tournament_name_snapshot<>? OR "
                    + seasonColumn + "<>?)", name, season, id, name, season);
        }
        for (String table : new String[]{"match_player_appearance_projection", "leaderboard_projection"}) {
            jdbc.update("UPDATE statistics." + table + " SET season=?, updated_at=CURRENT_TIMESTAMP WHERE tournament_id=? AND season<>?", season, id, season);
        }
        jdbc.update("UPDATE statistics.tournament_metadata_state SET last_applied_version=?, name=?, season=?, updated_at=CURRENT_TIMESTAMP WHERE tournament_id=?",
                version, name, season, id);
        if (playerMerged || rebuildBoards) leaderboards.rebuildLeaderboards(season, id);
        if (teamMerged) teamRanking.rebuildTournamentRanking(season, id);
    }

    private State lock(long id) {
        jdbc.update("INSERT INTO statistics.tournament_metadata_state(tournament_id,last_applied_version,updated_at) "
                + "VALUES (?,-1,CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING", id);
        return jdbc.queryForObject("SELECT last_applied_version,name,season FROM statistics.tournament_metadata_state WHERE tournament_id=? FOR UPDATE",
                (rs, n) -> new State(rs.getLong(1), rs.getString(2), rs.getString(3)), id);
    }

    private boolean collapse(String table, String key, long tournamentId, String... counters) {
        // SQL identifiers come exclusively from the private hard-coded call sites above.
        String sums = Arrays.stream(counters).map(c -> "SUM(" + c + ")").collect(Collectors.joining(","));
        var duplicates = jdbc.query("SELECT " + key + ",MIN(id)," + sums + " FROM statistics." + table
                + " WHERE tournament_id=? GROUP BY " + key + " HAVING COUNT(*)>1", (rs, n) -> {
            long[] values = new long[counters.length + 2];
            for (int i = 0; i < values.length; i++) values[i] = rs.getLong(i + 1);
            return values;
        }, tournamentId);
        for (long[] row : duplicates) {
            jdbc.update("DELETE FROM statistics." + table + " WHERE tournament_id=? AND " + key + "=? AND id<>?", tournamentId, row[0], row[1]);
            String assignments = Arrays.stream(counters).map(c -> c + "=?").collect(Collectors.joining(","));
            var args = new ArrayList<Object>();
            for (int i = 2; i < row.length; i++) args.add(row[i]);
            args.add(row[1]);
            jdbc.update("UPDATE statistics." + table + " SET " + assignments + " WHERE id=?", args.toArray());
        }
        return !duplicates.isEmpty();
    }

    private record State(long version, String name, String season) { }
}
