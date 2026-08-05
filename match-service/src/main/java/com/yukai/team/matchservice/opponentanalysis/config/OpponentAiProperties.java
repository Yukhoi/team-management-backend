package com.yukai.team.matchservice.opponentanalysis.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "opponent-analysis.ai")
public class OpponentAiProperties {

    private String provider = "openrouter";

    private String baseUrl = "https://openrouter.ai/api/v1";

    private String apiKey = "";

    private String model = "";

    @NotNull
    private Duration connectTimeout = Duration.ofSeconds(5);

    @NotNull
    private Duration readTimeout = Duration.ofSeconds(45);

    private Integer maxOutputTokens = 4000;

    private BigDecimal temperature = new BigDecimal("0.2");

    private String promptVersion = "v1";

    private boolean reasoningEnabled = false;

    private String httpReferer = "";

    private String title = "Team Management System";

    public boolean configured() {
        return apiKey != null && !apiKey.isBlank() && model != null && !model.isBlank();
    }
}
