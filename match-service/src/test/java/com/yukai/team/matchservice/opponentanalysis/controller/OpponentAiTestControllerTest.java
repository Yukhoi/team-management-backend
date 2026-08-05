package com.yukai.team.matchservice.opponentanalysis.controller;

import com.yukai.team.matchservice.context.CurrentUser;
import com.yukai.team.matchservice.context.UserContextHolder;
import com.yukai.team.matchservice.exception.GlobalExceptionHandler;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.GeneratedOpponentReport;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.OpponentReportGenerationResult;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.ThreatLevel;
import com.yukai.team.matchservice.opponentanalysis.exception.OpponentAiException;
import com.yukai.team.matchservice.opponentanalysis.service.OpponentReportGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpponentAiTestControllerTest {

    private OpponentReportGenerator opponentReportGenerator;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        opponentReportGenerator = mock(OpponentReportGenerator.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OpponentAiTestController(opponentReportGenerator))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void adminGeneratesReportFromFixedInput() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "admin", List.of("ADMIN")));
        when(opponentReportGenerator.generateWithMetadata(any())).thenReturn(result());

        mockMvc.perform(post("/api/v1/opponent-analysis/ai/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("openrouter"))
                .andExpect(jsonPath("$.model").value("test-model"))
                .andExpect(jsonPath("$.promptVersion").value("v1"))
                .andExpect(jsonPath("$.report.threatLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.report.summary").value("对手具备一定威胁。"))
                .andExpect(jsonPath("$.report.recommendations[0]").value("压缩中路空间"))
                .andExpect(jsonPath("$.usage.promptTokens").value(1))
                .andExpect(jsonPath("$.generatedAt").exists());
        verify(opponentReportGenerator).generateWithMetadata(any());
    }

    @Test
    void coachIsRejected() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "coach", List.of("COACH")));

        mockMvc.perform(post("/api/v1/opponent-analysis/ai/test"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(opponentReportGenerator);
    }

    @Test
    void playerIsRejected() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "player", List.of("PLAYER")));

        mockMvc.perform(post("/api/v1/opponent-analysis/ai/test"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(opponentReportGenerator);
    }

    @Test
    void mapsOpenRouterBadGatewayError() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "admin", List.of("ADMIN")));
        doThrow(new OpponentAiException(HttpStatus.BAD_GATEWAY, "AI_INVALID_RESPONSE", "OpenRouter returned invalid report JSON"))
                .when(opponentReportGenerator)
                .generateWithMetadata(any());

        mockMvc.perform(post("/api/v1/opponent-analysis/ai/test"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_INVALID_RESPONSE"));
    }

    @Test
    void mapsOpenRouterRateLimitError() throws Exception {
        UserContextHolder.set(new CurrentUser(1L, "admin", List.of("ADMIN")));
        doThrow(new OpponentAiException(HttpStatus.TOO_MANY_REQUESTS, "AI_RATE_LIMITED", "OpenRouter rate limit reached"))
                .when(opponentReportGenerator)
                .generateWithMetadata(any());

        mockMvc.perform(post("/api/v1/opponent-analysis/ai/test"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("AI_RATE_LIMITED"));
    }

    private OpponentReportGenerationResult result() {
        return new OpponentReportGenerationResult(
                "openrouter",
                "test-model",
                "v1",
                report(),
                new OpponentReportGenerationResult.Usage(1, 2, 3),
                OffsetDateTime.parse("2026-08-05T12:00:00Z")
        );
    }

    private GeneratedOpponentReport report() {
        return new GeneratedOpponentReport(
                ThreatLevel.MEDIUM,
                "对手具备一定威胁。",
                List.of(),
                List.of(),
                List.of(),
                List.of("压缩中路空间"),
                List.of("固定测试输入不包含球员数据。")
        );
    }
}
