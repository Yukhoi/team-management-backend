package com.yukai.team.identityservice.security;

import com.yukai.team.identityservice.config.JwtProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenAuthorizationHeaderMissing() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider());
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenTokenInvalid() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldSetAuthenticationWhenTokenValid() throws Exception {
        JwtTokenProvider tokenProvider = tokenProvider();
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider);
        String token = tokenProvider.generateAccessToken(1L, "admin", List.of("ROLE_ADMIN", "ROLE_COACH"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        JwtAuthenticationPrincipal principal = (JwtAuthenticationPrincipal) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
        assertEquals(1L, principal.getUserId());
        assertEquals("admin", principal.getUsername());
        assertEquals(List.of("ROLE_ADMIN", "ROLE_COACH"), principal.getRoles());
    }

    private JwtTokenProvider tokenProvider() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-with-at-least-thirty-two-bytes");
        properties.setAccessTokenExpirationMinutes(30L);
        properties.setRefreshTokenExpirationDays(7L);
        return new JwtTokenProvider(properties);
    }
}
