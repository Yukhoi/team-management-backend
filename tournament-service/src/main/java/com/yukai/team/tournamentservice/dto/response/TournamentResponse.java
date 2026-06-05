package com.yukai.team.tournamentservice.dto.response;

import com.yukai.team.tournamentservice.entity.enums.TournamentStatus;
import com.yukai.team.tournamentservice.entity.enums.TournamentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tournament response")
public class TournamentResponse {

    @Schema(description = "Tournament ID", example = "1")
    private Long id;
    @Schema(description = "Tournament name", example = "Summer League")
    private String name;
    @Schema(description = "Tournament type", example = "LEAGUE")
    private TournamentType tournamentType;
    @Schema(description = "Season label", example = "2026")
    private String season;
    @Schema(description = "Start date", example = "2026-06-01")
    private LocalDate startDate;
    @Schema(description = "End date", example = "2026-08-31")
    private LocalDate endDate;
    @Schema(description = "Organizer name", example = "City Association")
    private String organizer;
    @Schema(description = "Tournament description", example = "City summer league")
    private String description;
    @Schema(description = "Tournament status", example = "ACTIVE")
    private TournamentStatus status;
    @Schema(description = "Creation time", example = "2026-06-01T10:00:00Z")
    private OffsetDateTime createdAt;
    @Schema(description = "Last update time", example = "2026-06-01T11:00:00Z")
    private OffsetDateTime updatedAt;
    @Schema(description = "Optimistic lock version", example = "0")
    private Long version;
}
