package com.yukai.team.teamservice.dto.team;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Team response")
public class TeamResponse {

    @Schema(description = "Team ID", example = "1")
    private Long id;

    @Schema(description = "Team name", example = "Team A")
    private String name;

    @Schema(description = "Team short name", example = "TA")
    private String shortName;

    @Schema(description = "Team description", example = "Home team")
    private String description;

    @Schema(description = "Whether this is our team", example = "true")
    private Boolean isOurTeam;

    @Schema(description = "Remark", example = "Test team")
    private String remark;

    @Schema(description = "Created time", example = "2026-05-02T00:00:00+02:00")
    private OffsetDateTime createdAt;

    @Schema(description = "Updated time", example = "2026-05-02T00:00:00+02:00")
    private OffsetDateTime updatedAt;

    @Schema(description = "Optimistic lock version", example = "0")
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
