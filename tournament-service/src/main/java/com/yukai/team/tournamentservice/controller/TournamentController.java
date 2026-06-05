package com.yukai.team.tournamentservice.controller;

import com.yukai.team.tournamentservice.dto.request.CreateTournamentRequest;
import com.yukai.team.tournamentservice.dto.request.UpdateTournamentRequest;
import com.yukai.team.tournamentservice.dto.response.PageResponse;
import com.yukai.team.tournamentservice.dto.response.TournamentResponse;
import com.yukai.team.tournamentservice.entity.enums.TournamentStatus;
import com.yukai.team.tournamentservice.entity.enums.TournamentType;
import com.yukai.team.tournamentservice.service.TournamentService;
import com.yukai.team.tournamentservice.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/tournaments")
@Tag(name = "Tournaments", description = "Tournament lifecycle management APIs")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Created"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Tournament not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tournament state conflict", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @PostMapping({"", "/"})
    @Operation(summary = "Create tournament", description = "Create a tournament in active status")
    @ResponseStatus(HttpStatus.CREATED)
    public TournamentResponse createTournament(@Valid @RequestBody CreateTournamentRequest request) {
        return tournamentService.createTournament(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tournament", description = "Get tournament details by ID")
    public TournamentResponse getTournament(@PathVariable Long id) {
        return tournamentService.getTournament(id);
    }

    @GetMapping({"", "/"})
    @Operation(summary = "List tournaments", description = "List tournaments with optional season, status and type filters")
    public PageResponse<TournamentResponse> getTournaments(
            @RequestParam(required = false) String season,
            @RequestParam(required = false) TournamentStatus status,
            @RequestParam(required = false) TournamentType tournamentType,
            Pageable pageable
    ) {
        return tournamentService.getTournaments(season, status, tournamentType, pageable);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update tournament", description = "Update editable tournament details")
    public TournamentResponse updateTournament(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTournamentRequest request
    ) {
        return tournamentService.updateTournament(id, request);
    }

    @PatchMapping("/{id}/finish")
    @Operation(summary = "Finish tournament", description = "Mark an active tournament as finished")
    public TournamentResponse finishTournament(@PathVariable Long id) {
        return tournamentService.finishTournament(id);
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel tournament", description = "Mark an active tournament as cancelled")
    public TournamentResponse cancelTournament(@PathVariable Long id) {
        return tournamentService.cancelTournament(id);
    }
}
