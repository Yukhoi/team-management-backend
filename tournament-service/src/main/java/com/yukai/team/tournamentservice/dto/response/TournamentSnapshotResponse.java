package com.yukai.team.tournamentservice.dto.response;

import com.yukai.team.tournamentservice.entity.enums.TournamentStatus;
import com.yukai.team.tournamentservice.entity.enums.TournamentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Internal tournament snapshot response")
public class TournamentSnapshotResponse {

    @Schema(description = "Tournament ID", example = "1")
    private Long id;
    @Schema(description = "Tournament name", example = "Summer League")
    private String name;
    @Schema(description = "Season label", example = "2026")
    private String season;
    @Schema(description = "Tournament type", example = "LEAGUE")
    private TournamentType tournamentType;
    @Schema(description = "Tournament status", example = "ACTIVE")
    private TournamentStatus status;
}
