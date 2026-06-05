package com.yukai.team.teamservice.dto.player;

import com.yukai.team.teamservice.entity.PlayerCurrentStatus;
import com.yukai.team.teamservice.entity.PlayerPosition;
import com.yukai.team.teamservice.entity.PlayerRegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "Player response")
public class PlayerResponse {

    @Schema(description = "Player ID", example = "1")
    private Long id;

    @Schema(description = "Team ID", example = "1")
    private Long teamId;

    @Schema(description = "Team name", example = "Team A")
    private String teamName;

    @Schema(description = "Player name", example = "Alice")
    private String name;

    @Schema(description = "Jersey number", example = "10")
    private Integer jerseyNumber;

    @Schema(description = "Birth date", example = "2000-01-01")
    private LocalDate birthDate;

    @Schema(description = "Phone number", example = "13800138000")
    private String phone;

    @Schema(description = "Player position", example = "FORWARD")
    private PlayerPosition position;

    @Schema(description = "Registration status", example = "REGISTERED")
    private PlayerRegistrationStatus registrationStatus;

    @Schema(description = "Current status", example = "ACTIVE")
    private PlayerCurrentStatus currentStatus;

    @Schema(description = "Joined date", example = "2024-01-01")
    private LocalDate joinedDate;

    @Schema(description = "Left date", example = "2025-01-01")
    private LocalDate leftDate;

    @Schema(description = "Remark", example = "Starting forward")
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

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getJerseyNumber() {
        return jerseyNumber;
    }

    public void setJerseyNumber(Integer jerseyNumber) {
        this.jerseyNumber = jerseyNumber;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public PlayerPosition getPosition() {
        return position;
    }

    public void setPosition(PlayerPosition position) {
        this.position = position;
    }

    public PlayerRegistrationStatus getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(PlayerRegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public PlayerCurrentStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(PlayerCurrentStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public LocalDate getJoinedDate() {
        return joinedDate;
    }

    public void setJoinedDate(LocalDate joinedDate) {
        this.joinedDate = joinedDate;
    }

    public LocalDate getLeftDate() {
        return leftDate;
    }

    public void setLeftDate(LocalDate leftDate) {
        this.leftDate = leftDate;
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
