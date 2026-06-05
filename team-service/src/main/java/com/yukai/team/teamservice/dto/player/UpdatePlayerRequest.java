package com.yukai.team.teamservice.dto.player;

import com.yukai.team.teamservice.entity.PlayerPosition;
import com.yukai.team.teamservice.entity.PlayerRegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Update player request")
public class UpdatePlayerRequest {

    @Schema(description = "Player name", example = "Alice Updated")
    @NotBlank(message = "name must not be blank")
    @Size(max = 100, message = "name length must be less than or equal to 100")
    private String name;

    @Schema(description = "Jersey number", example = "11")
    @Min(value = 0, message = "jerseyNumber must be greater than or equal to 0")
    @Max(value = 999, message = "jerseyNumber must be less than or equal to 999")
    private Integer jerseyNumber;

    @Schema(description = "Birth date", example = "2000-01-01")
    private LocalDate birthDate;

    @Schema(description = "Phone number", example = "13900139000")
    @Size(max = 50, message = "phone length must be less than or equal to 50")
    private String phone;

    @Schema(description = "Player position", example = "MIDFIELDER")
    @NotNull(message = "position must not be null")
    private PlayerPosition position;

    @Schema(description = "Registration status", example = "REGISTERED")
    private PlayerRegistrationStatus registrationStatus;

    @Schema(description = "Joined date", example = "2024-01-01")
    private LocalDate joinedDate;

    @Schema(description = "Left date", example = "2025-01-01")
    private LocalDate leftDate;

    @Schema(description = "Remark", example = "Updated remark")
    private String remark;

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
}
