package com.yukai.team.auditservice.controller;

import com.yukai.team.auditservice.dto.AuditLogResponse;
import com.yukai.team.auditservice.dto.PageResponse;
import com.yukai.team.auditservice.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import com.yukai.team.auditservice.service.AuditQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/audit/logs")
@Tag(name = "Audit Logs", description = "Read-only audit operation log APIs")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Success"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Audit log not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class AuditQueryController {

    private final AuditQueryService auditQueryService;

    public AuditQueryController(AuditQueryService auditQueryService) {
        this.auditQueryService = auditQueryService;
    }

    @GetMapping
    @Operation(summary = "List audit logs", description = "List audit operation logs with pagination and optional filters. Accessible by ADMIN, COACH and PLAYER.")
    public PageResponse<AuditLogResponse> getLogs(
            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by event type", example = "match.goal.created")
            @RequestParam(required = false) String eventType,
            @Parameter(description = "Filter by aggregate type", example = "match")
            @RequestParam(required = false) String aggregateType,
            @Parameter(description = "Filter by aggregate ID", example = "1")
            @RequestParam(required = false) Long aggregateId,
            @Parameter(description = "Filter by operator username", example = "coach")
            @RequestParam(required = false) String username,
            @Parameter(description = "Filter by occurredAt start time", example = "2026-01-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @Parameter(description = "Filter by occurredAt end time", example = "2026-12-31T23:59:59Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return auditQueryService.getLogs(
                page,
                size,
                eventType,
                aggregateType,
                aggregateId,
                username,
                from,
                to
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get audit log", description = "Get an audit operation log by ID. Accessible by ADMIN, COACH and PLAYER.")
    public AuditLogResponse getLog(@Parameter(description = "Audit log ID", example = "1") @PathVariable Long id) {
        return auditQueryService.getLog(id);
    }
}
