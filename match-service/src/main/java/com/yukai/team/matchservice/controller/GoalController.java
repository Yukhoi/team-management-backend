package com.yukai.team.matchservice.controller;

import com.yukai.team.matchservice.dto.AssistResponse;
import com.yukai.team.matchservice.dto.CreateGoalRequest;
import com.yukai.team.matchservice.dto.GoalResponse;
import com.yukai.team.matchservice.dto.UpdateGoalRequest;
import com.yukai.team.matchservice.dto.UpsertAssistRequest;
import com.yukai.team.matchservice.service.GoalService;
import com.yukai.team.matchservice.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Goals", description = "Goal and assist management APIs")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Goal or match not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Goal state conflict", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping("/matches/{matchId}/goals")
    @Operation(summary = "Create goal", description = "Record a goal for a match. Requires ADMIN or COACH.")
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse createGoal(
            @Parameter(description = "Match ID", example = "1") @PathVariable Long matchId,
            @Valid @RequestBody CreateGoalRequest request
    ) {
        return goalService.createGoal(matchId, request);
    }

    @GetMapping("/matches/{matchId}/goals")
    @Operation(summary = "List match goals", description = "List goals and assists recorded for a match. Accessible by ADMIN, COACH and PLAYER. Returns an empty array when the match exists but has no goals.")
    public List<GoalResponse> getGoals(@Parameter(description = "Match ID", example = "1") @PathVariable Long matchId) {
        return goalService.getGoals(matchId);
    }

    @PatchMapping("/goals/{goalId}")
    @Operation(summary = "Update goal", description = "Update a recorded goal. Requires ADMIN or COACH.")
    public GoalResponse updateGoal(
            @Parameter(description = "Goal ID", example = "1") @PathVariable Long goalId,
            @Valid @RequestBody UpdateGoalRequest request
    ) {
        return goalService.updateGoal(goalId, request);
    }

    @DeleteMapping("/goals/{goalId}")
    @Operation(summary = "Delete goal", description = "Delete a recorded goal. Requires ADMIN or COACH.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGoal(@Parameter(description = "Goal ID", example = "1") @PathVariable Long goalId) {
        goalService.deleteGoal(goalId);
    }

    @PutMapping("/goals/{goalId}/assist")
    @Operation(summary = "Upsert assist", description = "Create or replace the assist associated with a goal. Requires ADMIN or COACH.")
    public AssistResponse upsertAssist(
            @Parameter(description = "Goal ID", example = "1") @PathVariable Long goalId,
            @Valid @RequestBody UpsertAssistRequest request
    ) {
        return goalService.upsertAssist(goalId, request);
    }

    @DeleteMapping("/goals/{goalId}/assist")
    @Operation(summary = "Delete assist", description = "Delete the assist associated with a goal. Requires ADMIN or COACH.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAssist(@Parameter(description = "Goal ID", example = "1") @PathVariable Long goalId) {
        goalService.deleteAssist(goalId);
    }
}
