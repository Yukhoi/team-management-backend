package com.yukai.team.statisticsservice.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@Tag(name = "Statistics", description = "Statistics projection query APIs")
@SecurityRequirement(name = "bearerAuth")
public class StatisticsHealthController {

    @GetMapping("/health")
    @Operation(summary = "Get statistics health", description = "Return a lightweight statistics-service health response")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Service is healthy"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Map<String, String> health() {
        return Map.of(
                "service", "statistics-service",
                "status", "UP"
        );
    }
}
