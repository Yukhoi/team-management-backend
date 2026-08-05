package com.yukai.team.matchservice.opponentanalysis.controller;

import com.yukai.team.matchservice.opponentanalysis.dto.FlaChampionnatResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaSyncResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamResponse;
import com.yukai.team.matchservice.context.CurrentUser;
import com.yukai.team.matchservice.context.UserContextHolder;
import com.yukai.team.matchservice.exception.GlobalExceptionHandler;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaClientException;
import com.yukai.team.matchservice.opponentanalysis.service.FlaCacheService;
import com.yukai.team.matchservice.opponentanalysis.service.FlaSyncService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FlaControllerTest {

    private FlaCacheService flaCacheService;
    private FlaSyncService flaSyncService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        flaCacheService = mock(FlaCacheService.class);
        flaSyncService = mock(FlaSyncService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FlaController(flaCacheService, flaSyncService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void getChampionnatsReturnsCachedChampionnats() throws Exception {
        when(flaCacheService.getChampionnats()).thenReturn(List.of(
                new FlaChampionnatResponse(1L, 101L, 2026L, "Division 1")
        ));

        mockMvc.perform(get("/api/v1/fla/championnats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].championnatId").value(101))
                .andExpect(jsonPath("$[0].saisonId").value(2026))
                .andExpect(jsonPath("$[0].name").value("Division 1"));
    }

    @Test
    void getTeamsReturnsCachedTeams() throws Exception {
        when(flaCacheService.getTeams(101L)).thenReturn(List.of(
                new FlaTeamResponse(2L, 101L, 2026L, 3001L, "A Team")
        ));

        mockMvc.perform(get("/api/v1/fla/championnats/101/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].championnatId").value(101))
                .andExpect(jsonPath("$[0].flaTeamId").value(3001))
                .andExpect(jsonPath("$[0].teamName").value("A Team"));

        verify(flaCacheService).getTeams(101L);
    }

    @Test
    void getTeamsBySeasonReturnsCachedTeams() throws Exception {
        when(flaCacheService.getTeams(101L, 2026L)).thenReturn(List.of(
                new FlaTeamResponse(2L, 101L, 2026L, 3001L, "A Team")
        ));

        mockMvc.perform(get("/api/v1/fla/championnats/101/seasons/2026/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].saisonId").value(2026))
                .andExpect(jsonPath("$[0].teamName").value("A Team"));

        verify(flaCacheService).getTeams(101L, 2026L);
    }

    @Test
    void syncStandingsReturnsSyncResponse() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));
        when(flaSyncService.syncStandings(1365L, 15L, "Division 3")).thenReturn(new FlaSyncResponse(
                1365L,
                15L,
                "Division 3",
                12,
                2,
                1,
                9,
                OffsetDateTime.parse("2026-08-04T21:30:00Z")
        ));

        mockMvc.perform(post("/api/v1/fla/championnats/1365/seasons/15/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"championnatName\":\"Division 3\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.championnatId").value(1365))
                .andExpect(jsonPath("$.saisonId").value(15))
                .andExpect(jsonPath("$.createdTeamCount").value(2))
                .andExpect(jsonPath("$.updatedTeamCount").value(1))
                .andExpect(jsonPath("$.unchangedTeamCount").value(9))
                .andExpect(jsonPath("$.syncedAt").exists());
    }

    @Test
    void syncStandingsRejectsInvalidParameters() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));

        mockMvc.perform(post("/api/v1/fla/championnats/0/seasons/15/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"championnatName\":\"Division 3\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void syncStandingsMapsExternalServiceException() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));
        doThrow(new FlaClientException(HttpStatus.BAD_GATEWAY, "FLA_UNAVAILABLE", "FLA service is unavailable"))
                .when(flaSyncService)
                .syncStandings(1365L, 15L, "Division 3");

        mockMvc.perform(post("/api/v1/fla/championnats/1365/seasons/15/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"championnatName\":\"Division 3\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("FLA_UNAVAILABLE"));
    }

    @Test
    void syncStandingsRequiresAdminOrCoach() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "player", List.of("PLAYER")));

        mockMvc.perform(post("/api/v1/fla/championnats/1365/seasons/15/sync")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"championnatName\":\"Division 3\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
