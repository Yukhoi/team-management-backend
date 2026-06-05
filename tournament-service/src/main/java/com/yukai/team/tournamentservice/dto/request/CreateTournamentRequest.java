package com.yukai.team.tournamentservice.dto.request;

import com.yukai.team.tournamentservice.entity.enums.TournamentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateTournamentRequest {

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Tournament name", example = "Summer League")
    private String name;

    @NotNull
    @Schema(description = "Tournament format", example = "LEAGUE")
    private TournamentType tournamentType;

    @NotBlank
    @Size(max = 30)
    @Schema(description = "Season label", example = "2026")
    private String season;

    @Schema(description = "Tournament start date", example = "2026-06-01")
    private LocalDate startDate;

    @Schema(description = "Tournament end date", example = "2026-08-31")
    private LocalDate endDate;

    @Size(max = 100)
    @Schema(description = "Organizer name", example = "City Association")
    private String organizer;

    @Schema(description = "Tournament description")
    private String description;
}
