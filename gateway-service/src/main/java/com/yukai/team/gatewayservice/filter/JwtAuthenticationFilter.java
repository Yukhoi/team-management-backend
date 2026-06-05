package com.yukai.team.gatewayservice.filter;

import com.yukai.team.gatewayservice.security.AuthenticatedUser;
import com.yukai.team.gatewayservice.security.JwtAuthenticationException;
import com.yukai.team.gatewayservice.security.JwtTokenValidator;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Component
public class JwtAuthenticationFilter implements WebFilter, Ordered {

    public static final String AUTHENTICATED_USER_ATTRIBUTE = "authenticatedUser";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MISSING_TOKEN_MESSAGE = "Missing access token";
    private static final String INVALID_TOKEN_MESSAGE = "Invalid or expired token";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    private static final Set<String> PUBLIC_POST_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/refresh"
    );

    private final JwtTokenValidator jwtTokenValidator;

    public JwtAuthenticationFilter(JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isPublicEndpoint(request)) {
            return chain.filter(exchange);
        }

        String token = extractBearerToken(request.getHeaders());
        if (token == null) {
            return Mono.error(new JwtAuthenticationException(MISSING_TOKEN_MESSAGE));
        }

        try {
            AuthenticatedUser user = jwtTokenValidator.validate(token);
            exchange.getAttributes().put(AUTHENTICATED_USER_ATTRIBUTE, user);

            ServerHttpRequest authenticatedRequest = request.mutate()
                    .headers(headers -> {
                        headers.set(USER_ID_HEADER, String.valueOf(user.userId()));
                        headers.set(USERNAME_HEADER, user.username());
                        headers.set(USER_ROLES_HEADER, joinRoles(user.roles()));
                    })
                    .build();

            return chain.filter(exchange.mutate().request(authenticatedRequest).build());
        } catch (JwtAuthenticationException exception) {
            return Mono.error(new JwtAuthenticationException(INVALID_TOKEN_MESSAGE, exception));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private boolean isPublicEndpoint(ServerHttpRequest request) {
        String path = request.getPath().pathWithinApplication().value();
        HttpMethod method = request.getMethod();

        if (HttpMethod.OPTIONS.equals(method)) {
            return true;
        }

        if (HttpMethod.GET.equals(method) && "/actuator/health".equals(path)) {
            return true;
        }

        if (HttpMethod.GET.equals(method) && isSwaggerEndpoint(path)) {
            return true;
        }

        return HttpMethod.POST.equals(method) && PUBLIC_POST_PATHS.contains(path);
    }

    private boolean isSwaggerEndpoint(String path) {
        return path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui/")
                || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/openapi/")
                || path.startsWith("/webjars/");
    }

    private String extractBearerToken(HttpHeaders headers) {
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private String joinRoles(List<String> roles) {
        return String.join(",", roles);
    }
}
