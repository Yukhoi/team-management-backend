package com.yukai.team.matchservice.opponentanalysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "FLA standings sync request")
public class FlaSyncRequest {

    @Size(max = 150)
    @Schema(description = "Championnat display name stored in local FLA cache", example = "Division 3")
    private String championnatName;
}
