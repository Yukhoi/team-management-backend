package com.yukai.team.matchservice.opponentanalysis.controller;

import com.yukai.team.matchservice.context.CurrentUser;
import com.yukai.team.matchservice.context.UserContextHolder;
import com.yukai.team.matchservice.exception.ErrorResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.FlaTeamMappingResponse;
import com.yukai.team.matchservice.opponentanalysis.dto.UpsertFlaTeamMappingRequest;
import com.yukai.team.matchservice.opponentanalysis.exception.FlaAccessDeniedException;
import com.yukai.team.matchservice.opponentanalysis.service.FlaTeamMappingService;
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
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/fla-mappings")
@Tag(name = "FLA Mappings", description = "System tournament/team to FLA team mapping APIs")
@SecurityRequirement(name = "bearerAuth")
@Validated
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Deleted"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mapping or referenced resource not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Mapping conflict", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class FlaTeamMappingController {

    private static final Set<String> READ_ROLES = Set.of("ADMIN", "COACH", "PLAYER");
    private static final Set<String> WRITE_ROLES = Set.of("ADMIN", "COACH");
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN");

    private final FlaTeamMappingService flaTeamMappingService;

    public FlaTeamMappingController(FlaTeamMappingService flaTeamMappingService) {
        this.flaTeamMappingService = flaTeamMappingService;
    }

    @PutMapping("/tournaments/{tournamentId}/teams/{teamId}")
    @Operation(summary = "Upsert FLA team mapping", description = "Create or update the mapping between an internal tournament/team and a cached FLA team. Requires ADMIN or COACH.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "FLA team identity from local cache",
            content = @Content(
                    schema = @Schema(implementation = UpsertFlaTeamMappingRequest.class),
                    examples = @ExampleObject(value = "{\"flaChampionnatId\":1365,\"flaSaisonId\":15,\"flaTeamId\":6580}")
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "FLA team mapping saved",
            content = @Content(
                    schema = @Schema(implementation = FlaTeamMappingResponse.class),
                    examples = @ExampleObject(value = "{\"id\":1,\"internalTournamentId\":5,\"internalTeamId\":12,\"internalTeamName\":\"YEXIAO PARIS FC\",\"flaChampionnatId\":1365,\"flaSaisonId\":15,\"flaTeamId\":6580,\"flaTeamName\":\"YEXIAO PARIS FC\",\"createdAt\":\"2026-08-04T21:30:00Z\",\"updatedAt\":\"2026-08-04T21:30:00Z\",\"version\":0}")
            )
    )
    public FlaTeamMappingResponse upsertMapping(
            @Parameter(description = "Internal tournament ID", example = "5") @PathVariable @Positive Long tournamentId,
            @Parameter(description = "Internal team ID", example = "12") @PathVariable @Positive Long teamId,
            @Valid @RequestBody UpsertFlaTeamMappingRequest request
    ) {
        requireRole(WRITE_ROLES, "ADMIN or COACH role is required to update FLA mappings");
        return flaTeamMappingService.upsertMapping(tournamentId, teamId, request);
    }

    @GetMapping("/tournaments/{tournamentId}")
    @Operation(summary = "List FLA team mappings", description = "List all FLA team mappings under an internal tournament. Accessible by ADMIN, COACH and PLAYER.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "FLA team mappings returned",
            content = @Content(
                    array = @ArraySchema(schema = @Schema(implementation = FlaTeamMappingResponse.class)),
                    examples = @ExampleObject(value = "[{\"id\":1,\"internalTournamentId\":5,\"internalTeamId\":12,\"internalTeamName\":\"YEXIAO PARIS FC\",\"flaChampionnatId\":1365,\"flaSaisonId\":15,\"flaTeamId\":6580,\"flaTeamName\":\"YEXIAO PARIS FC\",\"createdAt\":\"2026-08-04T21:30:00Z\",\"updatedAt\":\"2026-08-04T21:30:00Z\",\"version\":0}]")
            )
    )
    public List<FlaTeamMappingResponse> getMappings(
            @Parameter(description = "Internal tournament ID", example = "5") @PathVariable @Positive Long tournamentId
    ) {
        requireRole(READ_ROLES, "ADMIN, COACH or PLAYER role is required to view FLA mappings");
        return flaTeamMappingService.getMappings(tournamentId);
    }

    @GetMapping("/tournaments/{tournamentId}/teams/{teamId}")
    @Operation(summary = "Get FLA team mapping", description = "Get one FLA team mapping. Accessible by ADMIN, COACH and PLAYER.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "FLA team mapping returned",
            content = @Content(
                    schema = @Schema(implementation = FlaTeamMappingResponse.class),
                    examples = @ExampleObject(value = "{\"id\":1,\"internalTournamentId\":5,\"internalTeamId\":12,\"internalTeamName\":\"YEXIAO PARIS FC\",\"flaChampionnatId\":1365,\"flaSaisonId\":15,\"flaTeamId\":6580,\"flaTeamName\":\"YEXIAO PARIS FC\",\"createdAt\":\"2026-08-04T21:30:00Z\",\"updatedAt\":\"2026-08-04T21:30:00Z\",\"version\":0}")
            )
    )
    public FlaTeamMappingResponse getMapping(
            @Parameter(description = "Internal tournament ID", example = "5") @PathVariable @Positive Long tournamentId,
            @Parameter(description = "Internal team ID", example = "12") @PathVariable @Positive Long teamId
    ) {
        requireRole(READ_ROLES, "ADMIN, COACH or PLAYER role is required to view FLA mappings");
        return flaTeamMappingService.getMapping(tournamentId, teamId);
    }

    @DeleteMapping("/tournaments/{tournamentId}/teams/{teamId}")
    @Operation(summary = "Delete FLA team mapping", description = "Delete one FLA team mapping without deleting cached FLA team data. Requires ADMIN.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMapping(
            @Parameter(description = "Internal tournament ID", example = "5") @PathVariable @Positive Long tournamentId,
            @Parameter(description = "Internal team ID", example = "12") @PathVariable @Positive Long teamId
    ) {
        requireRole(ADMIN_ROLES, "ADMIN role is required to delete FLA mappings");
        flaTeamMappingService.deleteMapping(tournamentId, teamId);
    }

    private void requireRole(Set<String> allowedRoles, String message) {
        CurrentUser currentUser = UserContextHolder.get();
        if (currentUser == null || currentUser.roles() == null) {
            throw new FlaAccessDeniedException(message);
        }
        boolean allowed = currentUser.roles().stream().anyMatch(allowedRoles::contains);
        if (!allowed) {
            throw new FlaAccessDeniedException(message);
        }
    }
}
