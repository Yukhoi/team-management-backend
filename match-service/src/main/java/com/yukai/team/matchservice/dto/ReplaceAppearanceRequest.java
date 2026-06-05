package com.yukai.team.matchservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ReplaceAppearanceRequest {

    @Valid
    @NotNull
    @Schema(description = "Complete replacement appearance list")
    private List<PlayerAppearanceRequest> players;
}
