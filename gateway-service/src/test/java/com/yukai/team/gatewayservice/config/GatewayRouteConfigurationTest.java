package com.yukai.team.gatewayservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.handler.RoutePredicateHandlerMapping;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GatewayRouteConfigurationTest {

    @Autowired
    private RoutePredicateHandlerMapping routePredicateHandlerMapping;

    @Test
    void should_route_match_frontend_queries_to_match_service() {
        assertRoute("/api/v1/matches", "match-route", "http://localhost:8084");
        assertRoute("/api/v1/matches/1/goals", "match-route", "http://localhost:8084");
    }

    @Test
    void should_route_audit_frontend_queries_to_audit_service() {
        assertRoute("/api/audit/logs", "audit-route", "http://localhost:8083");
        assertRoute("/api/audit/logs/1", "audit-route", "http://localhost:8083");
    }

    @Test
    void should_route_aggregated_openapi_docs_to_services() {
        assertRoute("/openapi/identity/v3/api-docs", "identity-openapi-route", "http://localhost:8087");
        assertRoute("/openapi/team/v3/api-docs", "team-openapi-route", "http://localhost:8082");
        assertRoute("/openapi/tournament/v3/api-docs", "tournament-openapi-route", "http://localhost:8085");
        assertRoute("/openapi/match/v3/api-docs", "match-openapi-route", "http://localhost:8084");
        assertRoute("/openapi/statistics/v3/api-docs", "statistics-openapi-route", "http://localhost:8086");
        assertRoute("/openapi/audit/v3/api-docs", "audit-openapi-route", "http://localhost:8083");
    }

    private void assertRoute(String path, String routeId, String uri) {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(path));

        routePredicateHandlerMapping.getHandler(exchange).block();

        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        assertThat(route).isNotNull();
        assertThat(route.getId()).isEqualTo(routeId);
        assertThat(route.getUri()).hasToString(uri);
    }
}
