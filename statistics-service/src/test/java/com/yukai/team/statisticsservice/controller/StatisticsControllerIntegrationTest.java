package com.yukai.team.statisticsservice.controller;

import com.yukai.team.statisticsservice.entity.LeaderboardProjection;
import com.yukai.team.statisticsservice.entity.MatchSummaryProjection;
import com.yukai.team.statisticsservice.entity.PlayerStatsProjection;
import com.yukai.team.statisticsservice.entity.TeamStatsProjection;
import com.yukai.team.statisticsservice.entity.enums.HomeAwayType;
import com.yukai.team.statisticsservice.entity.enums.LeaderboardBoardType;
import com.yukai.team.statisticsservice.entity.enums.MatchStatus;
import com.yukai.team.statisticsservice.repository.LeaderboardProjectionRepository;
import com.yukai.team.statisticsservice.repository.MatchSummaryProjectionRepository;
import com.yukai.team.statisticsservice.repository.PlayerStatsProjectionRepository;
import com.yukai.team.statisticsservice.repository.TeamStatsProjectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:statistics-controller;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.properties.hibernate.default_schema=statistics",
        "spring.sql.init.mode=always",
        "spring.kafka.listener.auto-startup=false"
})
@AutoConfigureMockMvc
class StatisticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MatchSummaryProjectionRepository matchSummaryProjectionRepository;

    @Autowired
    private PlayerStatsProjectionRepository playerStatsProjectionRepository;

    @Autowired
    private LeaderboardProjectionRepository leaderboardProjectionRepository;

    @Autowired
    private TeamStatsProjectionRepository teamStatsProjectionRepository;

    @BeforeEach
    void setUp() {
        leaderboardProjectionRepository.deleteAll();
        playerStatsProjectionRepository.deleteAll();
        matchSummaryProjectionRepository.deleteAll();
        teamStatsProjectionRepository.deleteAll();
    }

    @Test
    void matchesPaginationReturnsCustomPageResponse() throws Exception {
        matchSummaryProjectionRepository.save(match(1L, 1L, "2026-05-20T20:00:00+02:00"));
        matchSummaryProjectionRepository.save(match(2L, 1L, "2026-05-21T20:00:00+02:00"));

        mockMvc.perform(get("/api/statistics/matches")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].matchId").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    @Test
    void matchesTournamentFilterReturnsOnlySelectedTournament() throws Exception {
        matchSummaryProjectionRepository.save(match(1L, 1L, "2026-05-20T20:00:00+02:00"));
        matchSummaryProjectionRepository.save(match(2L, 2L, "2026-05-21T20:00:00+02:00"));

        mockMvc.perform(get("/api/statistics/matches")
                        .param("tournamentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].tournamentId").value(1));
    }

    @Test
    void playersCanSortByGoals() throws Exception {
        playerStatsProjectionRepository.save(player(1L, "Player A", 4, 1, 1, 1, 5));
        playerStatsProjectionRepository.save(player(2L, "Player B", 2, 5, 1, 1, 6));

        mockMvc.perform(get("/api/statistics/players")
                        .param("season", "2026")
                        .param("tournamentId", "1")
                        .param("sortBy", "goals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].playerId").value(2))
                .andExpect(jsonPath("$.data.content[0].goals").value(5));
    }

    @Test
    void playersCanSortByGoalInvolvements() throws Exception {
        playerStatsProjectionRepository.save(player(1L, "Player A", 4, 1, 1, 1, 5));
        playerStatsProjectionRepository.save(player(2L, "Player B", 2, 5, 1, 1, 6));

        mockMvc.perform(get("/api/statistics/players")
                        .param("season", "2026")
                        .param("tournamentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].playerId").value(2))
                .andExpect(jsonPath("$.data.content[0].goalInvolvements").value(6));
    }

    @Test
    void leaderboardTopNTruncatesResults() throws Exception {
        leaderboardProjectionRepository.save(leaderboard(LeaderboardBoardType.SCORER, 1, 1L, "Player A", 5));
        leaderboardProjectionRepository.save(leaderboard(LeaderboardBoardType.SCORER, 2, 2L, "Player B", 4));

        mockMvc.perform(get("/api/statistics/leaderboards")
                        .param("boardType", "SCORER")
                        .param("season", "2026")
                        .param("tournamentId", "1")
                        .param("topN", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].rankNo").value(1))
                .andExpect(jsonPath("$.data[0].entityName").value("Player A"));
    }

    @Test
    void teamsReturnRankNoAscending() throws Exception {
        teamStatsProjectionRepository.save(team(1L, "Team A", 2, 3, 1, 4));
        teamStatsProjectionRepository.save(team(2L, "Team B", 1, 6, 2, 5));

        mockMvc.perform(get("/api/statistics/teams")
                        .param("season", "2026")
                        .param("tournamentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].teamId").value(2))
                .andExpect(jsonPath("$.data[0].rankNo").value(1))
                .andExpect(jsonPath("$.data[1].teamId").value(1));
    }

    @Test
    void dashboardReturnsLatestTournamentSummary() throws Exception {
        teamStatsProjectionRepository.save(team(1L, "Our Team", 1, 3, 1, 2));
        leaderboardProjectionRepository.save(leaderboard(LeaderboardBoardType.SCORER, 1, 10L, "Player A", 7));
        leaderboardProjectionRepository.save(leaderboard(LeaderboardBoardType.ASSIST, 1, 11L, "Player B", 4));

        mockMvc.perform(get("/api/statistics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalMatches").value(3))
                .andExpect(jsonPath("$.data.wins").value(2))
                .andExpect(jsonPath("$.data.draws").value(1))
                .andExpect(jsonPath("$.data.losses").value(0))
                .andExpect(jsonPath("$.data.goals").value(2))
                .andExpect(jsonPath("$.data.goalsAgainst").value(1))
                .andExpect(jsonPath("$.data.topScorer").value("Player A"))
                .andExpect(jsonPath("$.data.topScorerGoals").value(7))
                .andExpect(jsonPath("$.data.topAssist").value("Player B"))
                .andExpect(jsonPath("$.data.topAssistCount").value(4));
    }

    @Test
    void invalidBoardTypeReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/statistics/leaderboards")
                        .param("boardType", "UNKNOWN")
                        .param("season", "2026")
                        .param("tournamentId", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void invalidSortByReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/statistics/players")
                        .param("season", "2026")
                        .param("tournamentId", "1")
                        .param("sortBy", "points"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    private MatchSummaryProjection match(Long matchId, Long tournamentId, String matchTime) {
        return MatchSummaryProjection.builder()
                .matchId(matchId)
                .tournamentId(tournamentId)
                .tournamentNameSnapshot("Spring Cup")
                .seasonSnapshot("2026")
                .matchTime(OffsetDateTime.parse(matchTime))
                .opponentTeamNameSnapshot("Opponent " + matchId)
                .homeAway(HomeAwayType.HOME)
                .ourScore(2)
                .opponentScore(1)
                .matchStatus(MatchStatus.FINISHED)
                .finished(true)
                .build();
    }

    private PlayerStatsProjection player(
            Long playerId,
            String playerName,
            int appearances,
            int goals,
            int assists,
            int starts,
            int goalInvolvements
    ) {
        return PlayerStatsProjection.builder()
                .playerId(playerId)
                .playerNameSnapshot(playerName)
                .season("2026")
                .tournamentId(1L)
                .tournamentNameSnapshot("Spring Cup")
                .appearances(appearances)
                .starts(starts)
                .goals(goals)
                .assists(assists)
                .goalInvolvements(goalInvolvements)
                .build();
    }

    private LeaderboardProjection leaderboard(
            LeaderboardBoardType boardType,
            int rankNo,
            Long entityId,
            String entityName,
            int metricValue
    ) {
        return LeaderboardProjection.builder()
                .boardType(boardType)
                .season("2026")
                .tournamentId(1L)
                .rankNo(rankNo)
                .entityId(entityId)
                .entityNameSnapshot(entityName)
                .metricValue(metricValue)
                .build();
    }

    private TeamStatsProjection team(Long teamId, String teamName, int rankNo, int points, int goalDiff, int goalsFor) {
        return TeamStatsProjection.builder()
                .teamId(teamId)
                .teamNameSnapshot(teamName)
                .season("2026")
                .tournamentId(1L)
                .tournamentNameSnapshot("Spring Cup")
                .played(3)
                .win(2)
                .draw(1)
                .lose(0)
                .goalsFor(goalsFor)
                .goalsAgainst(goalsFor - goalDiff)
                .goalDiff(goalDiff)
                .points(points)
                .rankNo(rankNo)
                .build();
    }
}
