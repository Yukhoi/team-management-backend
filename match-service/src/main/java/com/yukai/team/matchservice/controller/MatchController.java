package com.yukai.team.matchservice.controller;

import com.yukai.team.matchservice.dto.CreateMatchRequest;
import com.yukai.team.matchservice.dto.MatchResponse;
import com.yukai.team.matchservice.dto.PageResponse;
import com.yukai.team.matchservice.dto.ReplaceAppearanceRequest;
import com.yukai.team.matchservice.dto.UpdateMatchResultRequest;
import com.yukai.team.matchservice.entity.MatchStatus;
import com.yukai.team.matchservice.service.MatchService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches")
@Tag(name = "Matches", description = "Match and appearance management APIs")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Match not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Match state conflict", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    @Operation(summary = "Create match", description = "Create a match and persist tournament and team snapshots. Requires ADMIN or COACH.")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchResponse createMatch(@Valid @RequestBody CreateMatchRequest request) {
        return matchService.createMatch(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get match", description = "Get match details and appearances by ID. Accessible by ADMIN, COACH and PLAYER.")
    public MatchResponse getMatch(@Parameter(description = "Match ID", example = "1") @PathVariable Long id) {
        return matchService.getMatch(id);
    }

    @GetMapping
    @Operation(summary = "List matches", description = "List matches with pagination and optional tournament, status and keyword filters. Accessible by ADMIN, COACH and PLAYER.")
    public PageResponse<MatchResponse> getMatches(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by tournament ID", example = "1")
            @RequestParam(required = false) Long tournamentId,
            @Parameter(description = "Filter by match status", example = "FINISHED")
            @RequestParam(required = false) MatchStatus status,
            @Parameter(description = "Keyword for opponent name, tournament name, venue or round/stage", example = "cup")
            @RequestParam(required = false) String keyword
    ) {
        return matchService.getMatches(page, size, tournamentId, status, keyword);
    }

    @PatchMapping("/{id}/result")
    @Operation(summary = "Update match result", description = "Update scores and finished state for a match. Requires ADMIN or COACH.")
    public MatchResponse updateResult(
            @Parameter(description = "Match ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateMatchResultRequest request
    ) {
        return matchService.updateResult(id, request);
    }

    @PutMapping("/{id}/appearances")
    @Operation(summary = "Replace appearances", description = "Replace the complete player appearance list for a match. Requires ADMIN or COACH.")
    public MatchResponse replaceAppearances(
            @Parameter(description = "Match ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ReplaceAppearanceRequest request
    ) {
        return matchService.replaceAppearances(id, request);
    }
}
