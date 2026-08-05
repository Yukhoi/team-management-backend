package com.yukai.team.matchservice.opponentanalysis.controller;

import com.yukai.team.matchservice.context.CurrentUser;
import com.yukai.team.matchservice.context.UserContextHolder;
import com.yukai.team.matchservice.exception.GlobalExceptionHandler;
import com.yukai.team.matchservice.opponentanalysis.dto.OpponentAnalysisReportResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.GeneratedOpponentReport;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.ThreatLevel;
import com.yukai.team.matchservice.opponentanalysis.entity.OpponentAnalysisStatus;
import com.yukai.team.matchservice.opponentanalysis.exception.OpponentAiException;
import com.yukai.team.matchservice.opponentanalysis.service.OpponentAnalysisApplicationService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpponentAnalysisReportControllerTest {

    private OpponentAnalysisApplicationService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(OpponentAnalysisApplicationService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OpponentAnalysisReportController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void adminCanGenerate() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "admin", List.of("ADMIN")));
        when(service.generate(42L, false)).thenReturn(response(false));

        mockMvc.perform(post("/api/v1/matches/42/opponent-analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.reused").value(false))
                .andExpect(jsonPath("$.report.threatLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.usage.promptTokens").value(11));
        verify(service).generate(42L, false);
    }

    @Test
    void coachCanGenerateWithForceRefresh() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));
        when(service.generate(42L, true)).thenReturn(response(false));

        mockMvc.perform(post("/api/v1/matches/42/opponent-analysis?forceRefresh=true"))
                .andExpect(status().isOk());
        verify(service).generate(42L, true);
    }

    @Test
    void playerCannotGenerate() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "player", List.of("PLAYER")));

        mockMvc.perform(post("/api/v1/matches/42/opponent-analysis"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(service);
    }

    @Test
    void playerCanViewLatest() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "player", List.of("PLAYER")));
        when(service.latest(42L)).thenReturn(response(false));

        mockMvc.perform(get("/api/v1/matches/42/opponent-analysis/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15));
    }

    @Test
    void latestMissingReturns404() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));
        doThrow(new EntityNotFoundException("not found")).when(service).latest(42L);

        mockMvc.perform(get("/api/v1/matches/42/opponent-analysis/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MATCH_NOT_FOUND"));
    }

    @Test
    void historyReturnsList() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));
        when(service.history(42L)).thenReturn(List.of(response(false), response(true)));

        mockMvc.perform(get("/api/v1/matches/42/opponent-analysis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(15))
                .andExpect(jsonPath("$[1].reused").value(true));
    }

    @Test
    void reportIdQueryReturnsReport() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));
        when(service.get(15L)).thenReturn(response(false));

        mockMvc.perform(get("/api/v1/opponent-analysis/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15));
    }

    @Test
    void aiRateLimitMapsTo429() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));
        doThrow(new OpponentAiException(HttpStatus.TOO_MANY_REQUESTS, "AI_RATE_LIMITED", "rate limited"))
                .when(service).generate(eq(42L), anyBoolean());

        mockMvc.perform(post("/api/v1/matches/42/opponent-analysis"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AI_RATE_LIMITED"));
    }

    @Test
    void aiBadGatewayMapsTo502() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));
        doThrow(new OpponentAiException(HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE", "invalid"))
                .when(service).generate(eq(42L), anyBoolean());

        mockMvc.perform(post("/api/v1/matches/42/opponent-analysis"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_INVALID_RESPONSE"));
    }

    @Test
    void aiTimeoutMapsTo504() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));
        doThrow(new OpponentAiException(HttpStatus.GATEWAY_TIMEOUT, "OPENROUTER_TIMEOUT", "timeout"))
                .when(service).generate(eq(42L), anyBoolean());

        mockMvc.perform(post("/api/v1/matches/42/opponent-analysis"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.code").value("OPENROUTER_TIMEOUT"));
    }

    private OpponentAnalysisReportResponse response(boolean reused) {
        return new OpponentAnalysisReportResponse(
                15L,
                42L,
                13L,
                "ASTERIA",
                OpponentAnalysisStatus.COMPLETED,
                "openrouter",
                "test-model",
                "v1",
                "zh-CN",
                reused,
                10L,
                OffsetDateTime.parse("2026-08-05T12:00:00Z"),
                OffsetDateTime.parse("2026-08-06T10:00:00Z"),
                OffsetDateTime.parse("2026-08-06T09:00:00Z"),
                null,
                new GeneratedOpponentReport(ThreatLevel.MEDIUM, "对手具备一定威胁。", List.of(), List.of(), List.of(), List.of("压缩空间"), List.of("无球员级别数据")),
                new OpponentAnalysisReportResponse.Usage(11, 22, 33),
                null
        );
    }
}
