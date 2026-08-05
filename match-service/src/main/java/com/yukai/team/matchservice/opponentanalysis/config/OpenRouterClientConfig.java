package com.yukai.team.matchservice.opponentanalysis.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OpponentAiProperties.class)
public class OpenRouterClientConfig {

    @Bean
    @Qualifier("openRouterRestClient")
    public RestClient openRouterRestClient(OpponentAiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (properties.configured()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey());
        }
        if (properties.getHttpReferer() != null && !properties.getHttpReferer().isBlank()) {
            builder.defaultHeader("HTTP-Referer", properties.getHttpReferer());
        }
        if (properties.getTitle() != null && !properties.getTitle().isBlank()) {
            builder.defaultHeader("X-OpenRouter-Title", properties.getTitle());
        }
        return builder.build();
    }
}
