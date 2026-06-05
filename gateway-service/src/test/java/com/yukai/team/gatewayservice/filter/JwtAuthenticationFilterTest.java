package com.yukai.team.gatewayservice.filter;

import com.yukai.team.gatewayservice.config.JwtProperties;
import com.yukai.team.gatewayservice.security.JwtTokenValidator;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.yukai.team.gatewayservice.security.JwtAuthenticationException;

class JwtAuthenticationFilterTest {

    @Test
    void should_skip_options_preflight_request_without_token() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("your-dev-secret-change-me-at-least-32-bytes");
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new JwtTokenValidator(jwtProperties));
        AtomicBoolean routed = new AtomicBoolean(false);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.OPTIONS, "/api/players"));

        assertDoesNotThrow(() -> filter.filter(exchange, ignored -> {
            routed.set(true);
            return reactor.core.publisher.Mono.empty();
        }).block());

        assertTrue(routed.get());
    }

    @Test
    void should_reject_protected_frontend_endpoint_without_token() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("your-dev-secret-change-me-at-least-32-bytes");
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new JwtTokenValidator(jwtProperties));
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/matches"));

        assertThrows(
                JwtAuthenticationException.class,
                () -> filter.filter(exchange, ignored -> reactor.core.publisher.Mono.empty()).block()
        );
    }

    @Test
    void should_skip_swagger_documentation_endpoint_without_token() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("your-dev-secret-change-me-at-least-32-bytes");
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(new JwtTokenValidator(jwtProperties));
        AtomicBoolean routed = new AtomicBoolean(false);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/swagger-ui.html"));

        assertDoesNotThrow(() -> filter.filter(exchange, ignored -> {
            routed.set(true);
            return reactor.core.publisher.Mono.empty();
        }).block());

        assertTrue(routed.get());
    }
}
