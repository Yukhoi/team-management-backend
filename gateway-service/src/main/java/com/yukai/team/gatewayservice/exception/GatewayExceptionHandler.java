package com.yukai.team.gatewayservice.exception;

import com.yukai.team.gatewayservice.security.AuthorizationException;
import com.yukai.team.gatewayservice.security.JwtAuthenticationException;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@Order(-2)
public class GatewayExceptionHandler implements WebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable exception) {
        if (exception instanceof JwtAuthenticationException) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, exception.getMessage());
        }

        if (exception instanceof AuthorizationException) {
            return writeError(exchange, HttpStatus.FORBIDDEN, exception.getMessage());
        }

        return Mono.error(exception);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }

        byte[] bytes = ("{\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
