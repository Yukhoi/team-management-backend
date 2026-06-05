package com.yukai.team.teamservice.dto.team;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Update team request")
public class UpdateTeamRequest {

    @Schema(description = "Team name", example = "Team A Updated")
    @NotBlank(message = "name must not be blank")
    @Size(max = 100, message = "name length must be less than or equal to 100")
    private String name;

    @Schema(description = "Team short name", example = "TAU")
    @Size(max = 50, message = "shortName length must be less than or equal to 50")
    private String shortName;

    @Schema(description = "Team description", example = "Updated team description")
    private String description;

    @Schema(description = "Whether this is our team; keep the current value when null", example = "true")
    private Boolean isOurTeam;

    @Schema(description = "Remark", example = "Updated remark")
    private String remark;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsOurTeam() {
        return isOurTeam;
    }

    public void setIsOurTeam(Boolean isOurTeam) {
        this.isOurTeam = isOurTeam;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
