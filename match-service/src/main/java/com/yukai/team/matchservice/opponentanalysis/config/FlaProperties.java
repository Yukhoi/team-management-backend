package com.yukai.team.matchservice.opponentanalysis.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "opponent-analysis.fla")
public class FlaProperties {

    @NotBlank
    private String baseUrl = "https://www.football-loisir-amateur.com";

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(5);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(20);

    @NotNull
    private Duration snapshotTtl = Duration.ofHours(6);
}
