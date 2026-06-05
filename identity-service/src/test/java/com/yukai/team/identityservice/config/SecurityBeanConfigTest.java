package com.yukai.team.identityservice.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityBeanConfigTest {

    @Test
    void shouldCreateBcryptPasswordEncoder() {
        PasswordEncoder passwordEncoder = new SecurityBeanConfig().passwordEncoder();

        String encoded = passwordEncoder.encode("secret-password");

        assertTrue(passwordEncoder.matches("secret-password", encoded));
    }
}
