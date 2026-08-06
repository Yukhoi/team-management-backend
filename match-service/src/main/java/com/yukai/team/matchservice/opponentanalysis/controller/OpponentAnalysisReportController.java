package com.yukai.team.matchservice.opponentanalysis.controller;

import com.yukai.team.matchservice.context.CurrentUser;
import com.yukai.team.matchservice.context.UserContextHolder;
import com.yukai.team.matchservice.exception.ErrorResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.OpponentAnalysisReportResponse;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaAccessDeniedException;
import com.yukai.team.matchservice.opponentanalysis.service.OpponentAnalysisApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Opponent Analysis", description = "AI opponent analysis generation, metrics and report query APIs")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class OpponentAnalysisReportController {

    private static final Set<String> GENERATE_ROLES = Set.of("ADMIN", "COACH");
    private static final Set<String> READ_ROLES = Set.of("ADMIN", "COACH", "PLAYER");
    private static final String REPORT_EXAMPLE = """
            {
              "id": 10,
              "matchId": 42,
              "opponentTeamId": 13,
              "opponentTeamName": "ASTERIA",
              "status": "COMPLETED",
              "provider": "openrouter",
              "model": "inclusionai/ling-3.0-flash",
              "promptVersion": "v1",
              "language": "zh-CN",
              "reused": false,
              "snapshotId": 23,
              "sourceFetchedAt": "2026-08-05T12:00:00Z",
              "generatedAt": "2026-08-06T10:15:30Z",
              "createdAt": "2026-08-06T10:15:20Z",
              "metrics": null,
              "usage": {
                "promptTokens": 1200,
                "completionTokens": 700,
                "totalTokens": 1900
              },
              "report": {
                "threatLevel": "HIGH",
                "summary": "对手整体实力明显强于我方，需要优先加强防守并减少中场失误。",
                "comparison": [
                  {
                    "metric": "联赛排名",
                    "ourValue": "12",
                    "opponentValue": "3",
                    "analysis": "对手排名明显更高。"
                  }
                ],
                "strengths": [
                  {
                    "title": "进攻火力强",
                    "analysis": "场均进球远高于联赛平均。",
                    "evidence": [
                      "场均进球4.31"
                    ]
                  }
                ],
                "weaknesses": [],
                "recommendations": [
                  "加强防守。",
                  "减少中场失误。"
                ],
                "dataLimitations": [
                  "无球员伤病数据。",
                  "无历史交锋数据。"
                ]
              },
              "errorCode": null
            }
            """;
    private static final String REPORT_HISTORY_EXAMPLE = "[" + REPORT_EXAMPLE + "]";

    private final OpponentAnalysisApplicationService service;

    public OpponentAnalysisReportController(OpponentAnalysisApplicationService service) {
        this.service = service;
    }

    @PostMapping("/matches/{matchId}/opponent-analysis")
    @Operation(
            summary = "Generate opponent analysis report",
            description = "根据比赛生成 AI 对手分析。使用 FLA 公共积分榜作为数据来源，使用 OpenRouter 生成中文结构化报告。默认 forceRefresh=false，允许复用相同 matchId、snapshotId、provider、model、promptVersion 和 language 下已有的 COMPLETED 报告；forceRefresh=true 会刷新 FLA 数据并强制重新生成。Requires ADMIN or COACH."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Generated or reused opponent analysis report", content = @Content(schema = @Schema(implementation = OpponentAnalysisReportResponse.class), examples = @ExampleObject(name = "completed", value = REPORT_EXAMPLE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied; ADMIN or COACH role is required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Match not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Match is not eligible or analysis context is invalid", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "OpenRouter rate limited", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "FLA or OpenRouter upstream error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "OpenRouter not configured", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "504", description = "FLA or OpenRouter timeout", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public OpponentAnalysisReportResponse generate(
            @Parameter(description = "Match ID", example = "42") @PathVariable("matchId") @Positive Long matchId,
            @Parameter(description = "Force refresh FLA data and force AI regeneration", example = "false")
            @RequestParam(defaultValue = "false") Boolean forceRefresh
    ) {
        validatePositive(matchId, "matchId");
        requireRole(GENERATE_ROLES, "ADMIN or COACH role is required to generate opponent analysis reports");
        return service.generate(matchId, Boolean.TRUE.equals(forceRefresh));
    }

    @GetMapping("/matches/{matchId}/opponent-analysis/latest")
    @Operation(summary = "Get latest opponent analysis report", description = "获取指定比赛的最新 AI 对手分析报告。Accessible by ADMIN, COACH and PLAYER.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Latest opponent analysis report", content = @Content(schema = @Schema(implementation = OpponentAnalysisReportResponse.class), examples = @ExampleObject(name = "completed", value = REPORT_EXAMPLE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied; ADMIN, COACH or PLAYER role is required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Match or completed report not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public OpponentAnalysisReportResponse latest(
            @Parameter(description = "Match ID", example = "42") @PathVariable("matchId") @Positive Long matchId
    ) {
        validatePositive(matchId, "matchId");
        requireRole(READ_ROLES, "ADMIN, COACH or PLAYER role is required to view opponent analysis reports");
        return service.latest(matchId);
    }

    @GetMapping("/matches/{matchId}/opponent-analysis")
    @Operation(summary = "List opponent analysis report history", description = "获取指定比赛的所有 AI 对手分析报告历史，按 createdAt DESC 排序。Accessible by ADMIN, COACH and PLAYER.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Opponent analysis report history", content = @Content(array = @ArraySchema(schema = @Schema(implementation = OpponentAnalysisReportResponse.class)), examples = @ExampleObject(name = "history", value = REPORT_HISTORY_EXAMPLE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied; ADMIN, COACH or PLAYER role is required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Match not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public List<OpponentAnalysisReportResponse> history(
            @Parameter(description = "Match ID", example = "42") @PathVariable("matchId") @Positive Long matchId
    ) {
        validatePositive(matchId, "matchId");
        requireRole(READ_ROLES, "ADMIN, COACH or PLAYER role is required to view opponent analysis reports");
        return service.history(matchId);
    }

    @GetMapping("/opponent-analysis/{reportId}")
    @Operation(summary = "Get opponent analysis report by ID", description = "根据 Report ID 获取 AI 对手分析报告详情。Accessible by ADMIN, COACH and PLAYER.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Opponent analysis report detail", content = @Content(schema = @Schema(implementation = OpponentAnalysisReportResponse.class), examples = @ExampleObject(name = "completed", value = REPORT_EXAMPLE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied; ADMIN, COACH or PLAYER role is required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Report not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public OpponentAnalysisReportResponse get(
            @Parameter(description = "Opponent analysis report ID", example = "10") @PathVariable("reportId") @Positive Long reportId
    ) {
        validatePositive(reportId, "reportId");
        requireRole(READ_ROLES, "ADMIN, COACH or PLAYER role is required to view opponent analysis reports");
        return service.get(reportId);
    }

    private void requireRole(Set<String> roles, String message) {
        CurrentUser currentUser = UserContextHolder.get();
        if (currentUser == null || currentUser.roles() == null) {
            throw new FlaAccessDeniedException(message);
        }
        boolean allowed = currentUser.roles().stream().anyMatch(roles::contains);
        if (!allowed) {
            throw new FlaAccessDeniedException(message);
        }
    }

    private void validatePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
