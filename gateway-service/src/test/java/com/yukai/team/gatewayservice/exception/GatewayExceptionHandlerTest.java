package com.yukai.team.gatewayservice.exception;

import com.yukai.team.gatewayservice.security.JwtAuthenticationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayExceptionHandlerTest {

    @Test
    void should_return_unauthorized_for_missing_access_token() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/matches"));

        new GatewayExceptionHandler()
                .handle(exchange, new JwtAuthenticationException("Missing access token"))
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
