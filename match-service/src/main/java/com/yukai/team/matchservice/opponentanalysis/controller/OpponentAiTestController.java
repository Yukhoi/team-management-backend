package com.yukai.team.matchservice.opponentanalysis.controller;

import com.yukai.team.matchservice.context.CurrentUser;
import com.yukai.team.matchservice.context.UserContextHolder;
import com.yukai.team.matchservice.entity.HomeAway;
import com.yukai.team.matchservice.exception.ErrorResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.OpponentAnalysisInput;
import com.yukai.team.matchservice.opponentanalysis.dto.ai.OpponentReportGenerationResult;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaAccessDeniedException;
import com.yukai.team.matchservice.opponentanalysis.service.OpponentReportGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@RestController
@Profile({"dev", "docker"})
@RequestMapping("/api/v1/opponent-analysis/ai")
@Tag(name = "Opponent Analysis", description = "AI opponent analysis generation, metrics and report query APIs")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "OpenRouter rate limited", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "OpenRouter upstream error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "OpenRouter is not configured", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "504", description = "OpenRouter request timeout", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class OpponentAiTestController {

    private static final Set<String> ROLES = Set.of("ADMIN");

    private final OpponentReportGenerator opponentReportGenerator;

    public OpponentAiTestController(OpponentReportGenerator opponentReportGenerator) {
        this.opponentReportGenerator = opponentReportGenerator;
    }

    @PostMapping("/test")
    @Operation(
            summary = "Test OpenRouter opponent report generation",
            description = "仅开发测试使用。This endpoint is enabled only under the dev and docker Spring profiles. It calls OpenRouter with fixed deterministic test data only; it does not use matchId, does not save OpponentAnalysisReport and does not generate report history. Requires ADMIN."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Generated structured opponent report",
            content = @Content(
                    schema = @Schema(implementation = OpponentReportGenerationResult.class),
                    examples = @ExampleObject(value = "{\"provider\":\"openrouter\",\"model\":\"test-model\",\"promptVersion\":\"v1\",\"report\":{\"threatLevel\":\"HIGH\",\"summary\":\"对手积分和攻防效率明显高于我方，需要优先降低其高频得分机会。\",\"comparison\":[{\"metric\":\"场均进球\",\"ourValue\":\"2.42\",\"opponentValue\":\"4.31\",\"analysis\":\"对手场均进球更高。\"}],\"strengths\":[{\"title\":\"对手进攻效率高\",\"analysis\":\"对手 26 场打进 112 球，场均 4.31 球。\",\"evidence\":[\"opponent.goalsForPerGame=4.31\"]}],\"weaknesses\":[],\"recommendations\":[\"我方应优先保护禁区前沿并减少开放空间。\"],\"dataLimitations\":[\"当前只包含积分榜统计，不包含球员、阵型和伤停信息。\"]},\"usage\":{\"promptTokens\":100,\"completionTokens\":200,\"totalTokens\":300},\"generatedAt\":\"2026-08-05T12:00:00Z\"}")
            )
    )
    public OpponentReportGenerationResult generateTestReport() {
        requireRole();
        return opponentReportGenerator.generateWithMetadata(testInput());
    }

    private void requireRole() {
        CurrentUser currentUser = UserContextHolder.get();
        if (currentUser == null || currentUser.roles() == null) {
            throw new FlaAccessDeniedException("ADMIN role is required to generate opponent AI test reports");
        }
        boolean allowed = currentUser.roles().stream().anyMatch(ROLES::contains);
        if (!allowed) {
            throw new FlaAccessDeniedException("ADMIN role is required to generate opponent AI test reports");
        }
    }

    private OpponentAnalysisInput testInput() {
        return new OpponentAnalysisInput(
                new OpponentAnalysisInput.MatchContext(
                        "FLA Division Test",
                        "2026",
                        OffsetDateTime.parse("2026-08-10T18:30:00Z"),
                        HomeAway.HOME
                ),
                new OpponentAnalysisInput.TeamMetrics(
                        "YEXIAO PARIS FC",
                        12,
                        20,
                        26,
                        6,
                        3,
                        17,
                        63,
                        134,
                        new BigDecimal("2.42"),
                        new BigDecimal("5.15"),
                        new BigDecimal("0.23"),
                        List.of("won", "lost", "lost", "won", "draw")
                ),
                new OpponentAnalysisInput.TeamMetrics(
                        "ASTERIA",
                        3,
                        68,
                        26,
                        22,
                        2,
                        2,
                        112,
                        40,
                        new BigDecimal("4.31"),
                        new BigDecimal("1.54"),
                        new BigDecimal("0.85"),
                        List.of("won", "won", "won", "draw", "won")
                ),
                new OpponentAnalysisInput.LeagueAverage(
                        new BigDecimal("2.58"),
                        new BigDecimal("2.58"),
                        null,
                        null
                ),
                new OpponentAnalysisInput.ComparisonMetrics(
                        -48,
                        9,
                        new BigDecimal("-0.62"),
                        null,
                        new BigDecimal("-1.89"),
                        new BigDecimal("3.61"),
                        -143,
                        -2,
                        new BigDecimal("-0.84"),
                        new BigDecimal("0.88")
                ),
                List.of("固定测试输入只包含积分榜统计，不包含球员、阵型、伤停、控球率、射门或历史交锋。")
        );
    }
}
