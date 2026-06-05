package com.yukai.team.identityservice.security;

import com.yukai.team.identityservice.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    @Test
    void shouldGenerateParseAndValidateAccessToken() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(jwtProperties());

        String token = tokenProvider.generateAccessToken(1L, "admin", List.of("ADMIN", "USER"));

        assertTrue(tokenProvider.validateToken(token));
        assertEquals("admin", tokenProvider.getUsername(token));
        assertEquals(1L, tokenProvider.getUserId(token));
        assertEquals(List.of("ADMIN", "USER"), tokenProvider.getRoles(token));
        assertEquals(1800L, tokenProvider.getExpirationSeconds());
    }

    @Test
    void shouldReturnFalseForInvalidToken() {
        JwtTokenProvider tokenProvider = new JwtTokenProvider(jwtProperties());

        assertFalse(tokenProvider.validateToken("invalid-token"));
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-with-at-least-thirty-two-bytes");
        properties.setAccessTokenExpirationMinutes(30L);
        properties.setRefreshTokenExpirationDays(7L);
        return properties;
    }
}
