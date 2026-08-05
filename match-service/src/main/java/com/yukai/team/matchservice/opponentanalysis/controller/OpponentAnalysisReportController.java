package com.yukai.team.matchservice.opponentanalysis.controller;

import com.yukai.team.matchservice.context.CurrentUser;
import com.yukai.team.matchservice.context.UserContextHolder;
import com.yukai.team.matchservice.exception.ErrorResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.OpponentAnalysisReportResponse;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaAccessDeniedException;
import com.yukai.team.matchservice.opponentanalysis.service.OpponentAnalysisApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Opponent Analysis Reports", description = "AI opponent analysis report generation and query APIs")
@SecurityRequirement(name = "bearerAuth")
@Validated
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Match or report not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Match is not eligible or analysis context is invalid", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "OpenRouter rate limited", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "FLA or OpenRouter upstream error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "OpenRouter not configured", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "504", description = "FLA or OpenRouter timeout", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class OpponentAnalysisReportController {

    private static final Set<String> GENERATE_ROLES = Set.of("ADMIN", "COACH");
    private static final Set<String> READ_ROLES = Set.of("ADMIN", "COACH", "PLAYER");

    private final OpponentAnalysisApplicationService service;

    public OpponentAnalysisReportController(OpponentAnalysisApplicationService service) {
        this.service = service;
    }

    @PostMapping("/matches/{matchId}/opponent-analysis")
    @Operation(
            summary = "Generate opponent analysis report",
            description = "Generates and persists a Chinese structured opponent analysis report for a scheduled match. forceRefresh=false may reuse the current FLA snapshot and an existing COMPLETED report with the same matchId, snapshotId, provider, model, promptVersion and language. forceRefresh=true refreshes FLA data and generates a new report. ADMIN and COACH only. The report is based on public FLA standings and is for reference only; AI has no player, formation, injury, suspension, shooting, possession or reliable head-to-head data."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Generated or reused opponent analysis report",
            content = @Content(
                    schema = @Schema(implementation = OpponentAnalysisReportResponse.class),
                    examples = @ExampleObject(value = "{\"id\":15,\"matchId\":42,\"opponentTeamId\":13,\"opponentTeamName\":\"ASTERIA\",\"status\":\"COMPLETED\",\"provider\":\"openrouter\",\"model\":\"inclusionai/ling-3.0-flash\",\"promptVersion\":\"v1\",\"language\":\"zh-CN\",\"reused\":false,\"snapshotId\":10,\"report\":{\"threatLevel\":\"HIGH\",\"summary\":\"对手积分和进攻效率明显高于我方。\",\"comparison\":[],\"strengths\":[{\"title\":\"对手进攻效率高\",\"analysis\":\"对手场均进球更高。\",\"evidence\":[\"opponent.goalsForPerGame=4.31\"]}],\"weaknesses\":[],\"recommendations\":[\"我方应优先压缩防线前空间。\"],\"dataLimitations\":[\"无球员级别数据\"]},\"usage\":{\"promptTokens\":100,\"completionTokens\":200,\"totalTokens\":300}}")
            )
    )
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
    @Operation(summary = "Get latest completed opponent analysis report", description = "Returns the latest COMPLETED opponent analysis report for a match. ADMIN, COACH and PLAYER can view.")
    public OpponentAnalysisReportResponse latest(@PathVariable("matchId") @Positive Long matchId) {
        validatePositive(matchId, "matchId");
        requireRole(READ_ROLES, "ADMIN, COACH or PLAYER role is required to view opponent analysis reports");
        return service.latest(matchId);
    }

    @GetMapping("/matches/{matchId}/opponent-analysis")
    @Operation(summary = "List opponent analysis report history", description = "Returns all opponent analysis report attempts for a match, ordered by createdAt DESC. ADMIN, COACH and PLAYER can view.")
    public List<OpponentAnalysisReportResponse> history(@PathVariable("matchId") @Positive Long matchId) {
        validatePositive(matchId, "matchId");
        requireRole(READ_ROLES, "ADMIN, COACH or PLAYER role is required to view opponent analysis reports");
        return service.history(matchId);
    }

    @GetMapping("/opponent-analysis/{reportId}")
    @Operation(summary = "Get opponent analysis report by ID", description = "Returns one opponent analysis report by ID. ADMIN, COACH and PLAYER can view.")
    public OpponentAnalysisReportResponse get(@PathVariable("reportId") @Positive Long reportId) {
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
