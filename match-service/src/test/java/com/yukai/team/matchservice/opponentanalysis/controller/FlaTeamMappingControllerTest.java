package com.yukai.team.matchservice.opponentanalysis.controller;

import com.yukai.team.matchservice.context.CurrentUser;
import com.yukai.team.matchservice.context.UserContextHolder;
import com.yukai.team.matchservice.exception.GlobalExceptionHandler;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMappingResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.UpsertFlaTeamMappingRequest;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaMappingConflictException;
import com.yukai.team.matchservice.opponentanalysis.service.FlaTeamMappingService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FlaTeamMappingControllerTest {

    private FlaTeamMappingService flaTeamMappingService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        flaTeamMappingService = mock(FlaTeamMappingService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FlaTeamMappingController(flaTeamMappingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void putUpsertsMapping() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));
        when(flaTeamMappingService.upsertMapping(any(), any(), any(UpsertFlaTeamMappingRequest.class)))
                .thenReturn(response());

        mockMvc.perform(put("/api/v1/fla-mappings/tournaments/5/teams/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flaChampionnatId\":1365,\"flaSaisonId\":15,\"flaTeamId\":6580}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.internalTournamentId").value(5))
                .andExpect(jsonPath("$.internalTeamId").value(12))
                .andExpect(jsonPath("$.flaTeamName").value("YEXIAO PARIS FC"));
    }

    @Test
    void getMappingsReturnsList() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "player", List.of("PLAYER")));
        when(flaTeamMappingService.getMappings(5L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/fla-mappings/tournaments/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].internalTeamName").value("YEXIAO PARIS FC"));
    }

    @Test
    void getMappingReturnsOne() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "player", List.of("PLAYER")));
        when(flaTeamMappingService.getMapping(5L, 12L)).thenReturn(response());

        mockMvc.perform(get("/api/v1/fla-mappings/tournaments/5/teams/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flaTeamId").value(6580));
    }

    @Test
    void deleteMappingReturnsNoContent() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "admin", List.of("ADMIN")));

        mockMvc.perform(delete("/api/v1/fla-mappings/tournaments/5/teams/12"))
                .andExpect(status().isNoContent());

        verify(flaTeamMappingService).deleteMapping(5L, 12L);
    }

    @Test
    void putValidatesRequest() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));

        mockMvc.perform(put("/api/v1/fla-mappings/tournaments/5/teams/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flaChampionnatId\":1365,\"flaSaisonId\":15}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void mapsNotFoundTo404() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "player", List.of("PLAYER")));
        doThrow(new EntityNotFoundException("FLA team mapping not found"))
                .when(flaTeamMappingService)
                .getMapping(5L, 12L);

        mockMvc.perform(get("/api/v1/fla-mappings/tournaments/5/teams/12"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_FOUND"));
    }

    @Test
    void mapsConflictTo409() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));
        doThrow(new FlaMappingConflictException("FLA team is already mapped in this tournament"))
                .when(flaTeamMappingService)
                .upsertMapping(any(), any(), any(UpsertFlaTeamMappingRequest.class));

        mockMvc.perform(put("/api/v1/fla-mappings/tournaments/5/teams/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flaChampionnatId\":1365,\"flaSaisonId\":15,\"flaTeamId\":6580}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FLA_MAPPING_CONFLICT"));
    }

    @Test
    void playerCannotPutMapping() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "player", List.of("PLAYER")));

        mockMvc.perform(put("/api/v1/fla-mappings/tournaments/5/teams/12")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"flaChampionnatId\":1365,\"flaSaisonId\":15,\"flaTeamId\":6580}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void coachCannotDeleteMapping() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));

        mockMvc.perform(delete("/api/v1/fla-mappings/tournaments/5/teams/12"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    private FlaTeamMappingResponse response() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-04T21:30:00Z");
        return new FlaTeamMappingResponse(
                1L,
                5L,
                12L,
                "YEXIAO PARIS FC",
                1365L,
                15L,
                6580L,
                "YEXIAO PARIS FC",
                now,
                now,
                0L
        );
    }
}
