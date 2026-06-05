package com.yukai.team.gatewayservice.security;

import com.yukai.team.gatewayservice.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtTokenValidatorTest {

    private static final String SECRET = "your-dev-secret-change-me-at-least-32-bytes";

    @Test
    void should_normalize_role_authority_prefix() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret(SECRET);
        JwtTokenValidator validator = new JwtTokenValidator(jwtProperties);

        AuthenticatedUser user = validator.validate(token(List.of("ROLE_ADMIN", "COACH")));

        assertEquals(List.of("ADMIN", "COACH"), user.roles());
    }

    private String token(List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("admin")
                .claim("userId", 1L)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(1800)))
                .signWith(signingKey())
                .compact();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
}
