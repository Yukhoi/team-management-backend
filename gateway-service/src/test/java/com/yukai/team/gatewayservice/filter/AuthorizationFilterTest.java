package com.yukai.team.gatewayservice.filter;

import com.yukai.team.gatewayservice.security.AuthenticatedUser;
import com.yukai.team.gatewayservice.security.AuthorizationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationFilterTest {

    private final AuthorizationFilter authorizationFilter = new AuthorizationFilter();

    @Test
    void should_allow_player_read_request() {
        AtomicBoolean routed = new AtomicBoolean(false);
        ServerWebExchange exchange = authenticatedExchange(HttpMethod.GET, "/api/players", List.of("PLAYER"));

        assertDoesNotThrow(() -> authorizationFilter.filter(exchange, successfulChain(routed)).block());

        assertTrue(routed.get());
    }

    @Test
    void should_allow_all_business_roles_to_read_new_frontend_endpoints() {
        for (String role : List.of("PLAYER", "COACH", "ADMIN")) {
            for (String path : List.of(
                    "/api/v1/matches",
                    "/api/v1/matches/1/goals",
                    "/api/audit/logs",
                    "/api/audit/logs/1"
            )) {
                AtomicBoolean routed = new AtomicBoolean(false);
                ServerWebExchange exchange = authenticatedExchange(HttpMethod.GET, path, List.of(role));

                assertDoesNotThrow(() -> authorizationFilter.filter(exchange, successfulChain(routed)).block());

                assertTrue(routed.get());
            }
        }
    }

    @Test
    void should_deny_player_write_request() {
        ServerWebExchange exchange = authenticatedExchange(HttpMethod.POST, "/api/players", List.of("PLAYER"));

        AuthorizationException exception = assertThrows(
                AuthorizationException.class,
                () -> authorizationFilter.filter(exchange, successfulChain(new AtomicBoolean(false))).block()
        );
        assertTrue("Access denied".equals(exception.getMessage()));
    }

    @Test
    void should_allow_coach_write_request() {
        AtomicBoolean routed = new AtomicBoolean(false);
        ServerWebExchange exchange = authenticatedExchange(
                HttpMethod.PATCH,
                "/api/v1/matches/1/result",
                List.of("COACH")
        );

        assertDoesNotThrow(() -> authorizationFilter.filter(exchange, successfulChain(routed)).block());

        assertTrue(routed.get());
    }

    @Test
    void should_allow_player_to_change_own_password() {
        AtomicBoolean routed = new AtomicBoolean(false);
        ServerWebExchange exchange = authenticatedExchange(
                HttpMethod.POST,
                "/api/auth/change-password",
                List.of("PLAYER")
        );

        assertDoesNotThrow(() -> authorizationFilter.filter(exchange, successfulChain(routed)).block());

        assertTrue(routed.get());
    }

    @Test
    void should_allow_player_to_logout() {
        AtomicBoolean routed = new AtomicBoolean(false);
        ServerWebExchange exchange = authenticatedExchange(
                HttpMethod.POST,
                "/api/auth/logout",
                List.of("PLAYER")
        );

        assertDoesNotThrow(() -> authorizationFilter.filter(exchange, successfulChain(routed)).block());

        assertTrue(routed.get());
    }

    @Test
    void should_skip_options_preflight_request() {
        AtomicBoolean routed = new AtomicBoolean(false);
        ServerWebExchange exchange = exchange(HttpMethod.OPTIONS, "/api/players");

        assertDoesNotThrow(() -> authorizationFilter.filter(exchange, successfulChain(routed)).block());

        assertTrue(routed.get());
    }

    @Test
    void should_allow_admin_identity_management_request() {
        AtomicBoolean routed = new AtomicBoolean(false);
        ServerWebExchange exchange = authenticatedExchange(HttpMethod.POST, "/api/users", List.of("ADMIN"));

        assertDoesNotThrow(() -> authorizationFilter.filter(exchange, successfulChain(routed)).block());

        assertTrue(routed.get());
    }

    @Test
    void should_deny_coach_identity_management_request() {
        ServerWebExchange exchange = authenticatedExchange(HttpMethod.POST, "/api/users", List.of("COACH"));

        AuthorizationException exception = assertThrows(
                AuthorizationException.class,
                () -> authorizationFilter.filter(exchange, successfulChain(new AtomicBoolean(false))).block()
        );
        assertTrue("Access denied".equals(exception.getMessage()));
    }

    @Test
    void should_skip_public_endpoint() {
        AtomicBoolean routed = new AtomicBoolean(false);
        ServerWebExchange exchange = exchange(HttpMethod.POST, "/api/auth/login");

        assertDoesNotThrow(() -> authorizationFilter.filter(exchange, successfulChain(routed)).block());

        assertTrue(routed.get());
    }

    @Test
    void should_skip_swagger_documentation_endpoint() {
        AtomicBoolean routed = new AtomicBoolean(false);
        ServerWebExchange exchange = exchange(HttpMethod.GET, "/v3/api-docs");

        assertDoesNotThrow(() -> authorizationFilter.filter(exchange, successfulChain(routed)).block());

        assertTrue(routed.get());
    }

    private ServerWebExchange authenticatedExchange(HttpMethod method, String path, List<String> roles) {
        ServerWebExchange exchange = exchange(method, path);
        exchange.getAttributes().put(
                JwtAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE,
                new AuthenticatedUser(1L, "user", roles)
        );
        return exchange;
    }

    private ServerWebExchange exchange(HttpMethod method, String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.method(method, path));
    }

    private WebFilterChain successfulChain(AtomicBoolean routed) {
        return exchange -> {
            routed.set(true);
            return Mono.empty();
        };
    }
}
