package com.yukai.team.matchservice.opponentanalysis.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlaStandingsRequest {

    @NotNull
    @Positive
    private Long championnatId;

    @NotNull
    @Positive
    private Long saisonId;
}
