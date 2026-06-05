package com.yukai.team.gatewayservice.filter;

import com.yukai.team.gatewayservice.security.AuthenticatedUser;
import com.yukai.team.gatewayservice.security.AuthorizationException;
import org.springframework.core.Ordered;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Component
public class AuthorizationFilter implements WebFilter, Ordered {

    private static final String ACCESS_DENIED_MESSAGE = "Access denied";
    private static final Set<String> READ_ROLES = Set.of("ADMIN", "COACH", "PLAYER");
    private static final Set<String> WRITE_ROLES = Set.of("ADMIN", "COACH");
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN");

    // By default allow swagger endpoints to be publicly accessible (development).
    // In production override gateway.swagger.public=false to require ADMIN role for swagger endpoints.
    private final boolean swaggerPublic;

    private static final Set<String> PUBLIC_POST_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/refresh"
    );
    private static final Set<String> SELF_SERVICE_POST_PATHS = Set.of(
            "/api/auth/change-password",
            "/api/auth/logout"
    );

    public AuthorizationFilter(@Value("${gateway.swagger.public:true}") boolean swaggerPublic) {
        this.swaggerPublic = swaggerPublic;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (isPublicEndpoint(request)) {
            return chain.filter(exchange);
        }

        AuthenticatedUser user = exchange.getAttribute(JwtAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE);
        if (user == null || !isAllowed(request, user.roles())) {
            return Mono.error(new AuthorizationException(ACCESS_DENIED_MESSAGE));
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    private boolean isAllowed(ServerHttpRequest request, List<String> roles) {
        String path = request.getPath().pathWithinApplication().value();
        HttpMethod method = request.getMethod();

        // If this is a swagger/openapi endpoint and swaggerPublic is false, require ADMIN role
        if (isSwaggerEndpoint(path)) {
            return hasAnyRole(roles, ADMIN_ROLES);
        }

        if (isIdentityManagementPath(path)) {
            return hasAnyRole(roles, ADMIN_ROLES);
        }

        if (HttpMethod.POST.equals(method) && SELF_SERVICE_POST_PATHS.contains(path)) {
            return hasAnyRole(roles, READ_ROLES);
        }

        if (HttpMethod.GET.equals(method)) {
            return hasAnyRole(roles, READ_ROLES);
        }

        if (isWriteMethod(method)) {
            return hasAnyRole(roles, WRITE_ROLES);
        }

        return false;
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

        // Swagger endpoints are considered public only when swaggerPublic is true (dev/docker).
        if (HttpMethod.GET.equals(method) && isSwaggerEndpoint(path)) {
            return this.swaggerPublic;
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

    private boolean isIdentityManagementPath(String path) {
        return isPathOrChild(path, "/api/users") || isPathOrChild(path, "/api/roles");
    }

    private boolean isPathOrChild(String path, String basePath) {
        return path.equals(basePath) || path.startsWith(basePath + "/");
    }

    private boolean isWriteMethod(HttpMethod method) {
        return HttpMethod.POST.equals(method)
                || HttpMethod.PUT.equals(method)
                || HttpMethod.PATCH.equals(method)
                || HttpMethod.DELETE.equals(method);
    }

    private boolean hasAnyRole(List<String> roles, Set<String> allowedRoles) {
        return roles.stream().anyMatch(allowedRoles::contains);
    }
}
