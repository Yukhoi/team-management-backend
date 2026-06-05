package com.yukai.team.identityservice.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentJwtPrincipalValues() {
        JwtAuthenticationPrincipal principal = JwtAuthenticationPrincipal.builder()
                .userId(1L)
                .username("admin")
                .roles(List.of("ROLE_ADMIN"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        );

        assertEquals(1L, SecurityUtils.getCurrentUserId());
        assertEquals("admin", SecurityUtils.getCurrentUsername());
        assertEquals(List.of("ROLE_ADMIN"), SecurityUtils.getCurrentRoles());
        assertTrue(SecurityUtils.hasRole("ADMIN"));
        assertTrue(SecurityUtils.hasRole("ROLE_ADMIN"));
        assertFalse(SecurityUtils.hasRole("COACH"));
        assertTrue(SecurityUtils.isCurrentUser(1L));
        assertFalse(SecurityUtils.isCurrentUser(2L));
    }

    @Test
    void shouldReturnEmptyValuesWhenNotAuthenticated() {
        assertNull(SecurityUtils.getCurrentUserId());
        assertNull(SecurityUtils.getCurrentUsername());
        assertEquals(List.of(), SecurityUtils.getCurrentRoles());
        assertFalse(SecurityUtils.hasRole("ADMIN"));
        assertFalse(SecurityUtils.isCurrentUser(1L));
    }
}
