package com.yukai.team.matchservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI matchServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Match Service API")
                        .version("1.0.0")
                        .description("Match, appearance, goal, assist and opponent analysis APIs")
                        .contact(new Contact().name("Team Management Backend"))
                        .license(new License().name("Proprietary")))
                .servers(List.of(new Server().url("http://localhost:8084").description("Local Match Service")))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")));
    }

    @Bean
    @SuppressWarnings("unchecked")
    public OpenApiCustomizer opponentAnalysisSchemaCustomizer() {
        return openApi -> openApi.getComponents()
                .addSchemas("ThreatLevel", new StringSchema()
                        .description("Opponent threat level")
                        ._enum(List.of("LOW", "MEDIUM", "HIGH"))
                        .example("HIGH"))
                .addSchemas("Status", new StringSchema()
                        .description("Opponent analysis report status")
                        ._enum(List.of("PENDING", "COMPLETED", "FAILED"))
                        .example("COMPLETED"));
    }
}
