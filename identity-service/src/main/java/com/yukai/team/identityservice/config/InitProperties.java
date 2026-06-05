package com.yukai.team.identityservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.init")
public class InitProperties {

    private String adminUsername;
    private String adminPassword;
    private String adminDisplayName;
}
