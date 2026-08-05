package com.yukai.team.matchservice.opponentanalysis.controller;

import com.yukai.team.matchservice.exception.ErrorResponse;
import com.yukai.team.matchservice.context.CurrentUser;
import com.yukai.team.matchservice.context.UserContextHolder;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaChampionnatResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaSyncRequest;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaSyncResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamResponse;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaAccessDeniedException;
import com.yukai.team.matchservice.opponentanalysis.service.FlaCacheService;
import com.yukai.team.matchservice.opponentanalysis.service.FlaSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/fla")
@Tag(name = "FLA", description = "FLA cached reference data APIs")
@SecurityRequirement(name = "bearerAuth")
@Validated
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "FLA upstream error", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "504", description = "FLA upstream timeout", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class FlaController {

    private static final Set<String> SYNC_ROLES = Set.of("ADMIN", "COACH");

    private final FlaCacheService flaCacheService;
    private final FlaSyncService flaSyncService;

    public FlaController(FlaCacheService flaCacheService, FlaSyncService flaSyncService) {
        this.flaCacheService = flaCacheService;
        this.flaSyncService = flaSyncService;
    }

    @GetMapping("/championnats")
    @Operation(summary = "List FLA championnats", description = "List all cached FLA championnats for selection and later analysis workflows.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "FLA championnats returned",
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = FlaChampionnatResponse.class)),
                    examples = @ExampleObject(value = "[{\"id\":1,\"championnatId\":101,\"saisonId\":2026,\"name\":\"Football Loisir Amateur - Division 1\"}]")
            )
    )
    public List<FlaChampionnatResponse> getChampionnats() {
        return flaCacheService.getChampionnats();
    }

    @GetMapping("/championnats/{championnatId}/teams")
    @Operation(summary = "List FLA teams", description = "List all cached FLA teams under a championnat, sorted by team name.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "FLA teams returned",
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = FlaTeamResponse.class)),
                    examples = @ExampleObject(value = "[{\"id\":1,\"championnatId\":101,\"saisonId\":2026,\"flaTeamId\":3001,\"teamName\":\"Paris FC Loisir\"}]")
            )
    )
    public List<FlaTeamResponse> getTeams(
            @Parameter(description = "FLA championnat ID", example = "101") @PathVariable @Positive Long championnatId
    ) {
        return flaCacheService.getTeams(championnatId);
    }

    @GetMapping("/championnats/{championnatId}/seasons/{saisonId}/teams")
    @Operation(summary = "List FLA teams by season", description = "List all cached FLA teams under a championnat and season, sorted by team name.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "FLA teams returned",
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = FlaTeamResponse.class)),
                    examples = @ExampleObject(value = "[{\"id\":1,\"championnatId\":1365,\"saisonId\":15,\"flaTeamId\":6580,\"teamName\":\"YEXIAO PARIS FC\"}]")
            )
    )
    public List<FlaTeamResponse> getTeamsBySeason(
            @Parameter(description = "FLA championnat ID", example = "1365") @PathVariable @Positive Long championnatId,
            @Parameter(description = "FLA saison ID", example = "15") @PathVariable @Positive Long saisonId
    ) {
        return flaCacheService.getTeams(championnatId, saisonId);
    }

    @PostMapping("/championnats/{championnatId}/seasons/{saisonId}/sync")
    @Operation(summary = "Sync FLA standings", description = "Fetch FLA standings and upsert cached championnat and team rows. Requires ADMIN or COACH.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Championnat metadata for the local cache",
            content = @Content(
                    schema = @Schema(implementation = FlaSyncRequest.class),
                    examples = @ExampleObject(value = "{\"championnatName\":\"Division 3\"}")
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "FLA standings synchronized",
            content = @Content(
                    schema = @Schema(implementation = FlaSyncResponse.class),
                    examples = @ExampleObject(value = "{\"championnatId\":1365,\"saisonId\":15,\"championnatName\":\"Division 3\",\"receivedTeamCount\":12,\"createdTeamCount\":2,\"updatedTeamCount\":1,\"unchangedTeamCount\":9,\"syncedAt\":\"2026-08-04T21:30:00Z\"}")
            )
    )
    public FlaSyncResponse syncStandings(
            @Parameter(description = "FLA championnat ID", example = "1365") @PathVariable @Positive Long championnatId,
            @Parameter(description = "FLA saison ID", example = "15") @PathVariable @Positive Long saisonId,
            @Valid @RequestBody(required = false) FlaSyncRequest request
    ) {
        validatePositive(championnatId, "championnatId");
        validatePositive(saisonId, "saisonId");
        requireSyncRole();
        String championnatName = request == null ? null : request.getChampionnatName();
        return flaSyncService.syncStandings(championnatId, saisonId, championnatName);
    }

    private void validatePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    private void requireSyncRole() {
        CurrentUser currentUser = UserContextHolder.get();
        if (currentUser == null || currentUser.roles() == null) {
            throw new FlaAccessDeniedException("ADMIN or COACH role is required to sync FLA data");
        }
        boolean allowed = currentUser.roles().stream().anyMatch(SYNC_ROLES::contains);
        if (!allowed) {
            throw new FlaAccessDeniedException("ADMIN or COACH role is required to sync FLA data");
        }
    }
}
