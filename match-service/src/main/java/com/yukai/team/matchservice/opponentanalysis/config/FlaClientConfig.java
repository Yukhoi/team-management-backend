package com.yukai.team.matchservice.opponentanalysis.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableConfigurationProperties(FlaProperties.class)
public class FlaClientConfig {

    @Bean
    @Qualifier("flaRestClient")
    public RestClient flaRestClient(FlaProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());

        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                // Preserve French headings/names when HTML omits an HTTP charset.
                .configureMessageConverters(converters -> converters.withStringConverter(
                        new StringHttpMessageConverter(StandardCharsets.UTF_8)))
                .build();
    }
}
