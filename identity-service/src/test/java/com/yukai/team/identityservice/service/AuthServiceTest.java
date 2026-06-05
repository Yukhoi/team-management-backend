package com.yukai.team.identityservice.service;

import com.yukai.team.identityservice.config.JwtProperties;
import com.yukai.team.identityservice.dto.request.ChangePasswordRequest;
import com.yukai.team.identityservice.dto.request.LoginRequest;
import com.yukai.team.identityservice.dto.request.LogoutRequest;
import com.yukai.team.identityservice.dto.request.RefreshTokenRequest;
import com.yukai.team.identityservice.dto.response.LoginResponse;
import com.yukai.team.identityservice.dto.response.TokenRefreshResponse;
import com.yukai.team.identityservice.entity.RefreshTokenEntity;
import com.yukai.team.identityservice.entity.RoleEntity;
import com.yukai.team.identityservice.entity.UserAccountEntity;
import com.yukai.team.identityservice.entity.UserRoleEntity;
import com.yukai.team.identityservice.enums.UserStatus;
import com.yukai.team.identityservice.repository.RefreshTokenRepository;
import com.yukai.team.identityservice.repository.UserAccountRepository;
import com.yukai.team.identityservice.repository.UserRoleRepository;
import com.yukai.team.identityservice.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    @Test
    void shouldLoginAndReturnTokens() {
        TestContext context = testContext();

        LoginResponse response = context.authService.login(LoginRequest.builder()
                .username("admin")
                .password("old-password")
                .build());

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(1800L, response.getExpiresIn());
        assertTrue(context.jwtTokenProvider.validateToken(response.getAccessToken()));
        assertEquals("admin", context.jwtTokenProvider.getUsername(response.getAccessToken()));
        assertEquals(List.of("ROLE_ADMIN"), context.jwtTokenProvider.getRoles(response.getAccessToken()));
        assertNotNull(context.userRepository.user.getLastLoginAt());
        assertFalse(context.refreshTokenRepository.tokens.get(response.getRefreshToken()).getRevokedFlag());
    }

    @Test
    void shouldRefreshAccessTokenWithoutCreatingNewRefreshToken() {
        TestContext context = testContext();
        LoginResponse loginResponse = context.authService.login(LoginRequest.builder()
                .username("admin")
                .password("old-password")
                .build());
        int refreshTokenCount = context.refreshTokenRepository.tokens.size();

        TokenRefreshResponse response = context.authService.refreshToken(RefreshTokenRequest.builder()
                .refreshToken(loginResponse.getRefreshToken())
                .build());

        assertNotNull(response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(1800L, response.getExpiresIn());
        assertTrue(context.jwtTokenProvider.validateToken(response.getAccessToken()));
        assertEquals(refreshTokenCount, context.refreshTokenRepository.tokens.size());
    }

    @Test
    void shouldLogoutByRevokingRefreshToken() {
        TestContext context = testContext();
        LoginResponse loginResponse = context.authService.login(LoginRequest.builder()
                .username("admin")
                .password("old-password")
                .build());

        context.authService.logout(LogoutRequest.builder()
                .refreshToken(loginResponse.getRefreshToken())
                .build());

        assertTrue(context.refreshTokenRepository.tokens.get(loginResponse.getRefreshToken()).getRevokedFlag());
    }

    @Test
    void shouldChangePasswordAndRevokeAllRefreshTokens() {
        TestContext context = testContext();
        LoginResponse firstLogin = context.authService.login(LoginRequest.builder()
                .username("admin")
                .password("old-password")
                .build());
        LoginResponse secondLogin = context.authService.login(LoginRequest.builder()
                .username("admin")
                .password("old-password")
                .build());

        context.authService.changePassword(1L, ChangePasswordRequest.builder()
                .oldPassword("old-password")
                .newPassword("new-password")
                .build());

        assertTrue(context.passwordEncoder.matches("new-password", context.userRepository.user.getPasswordHash()));
        assertTrue(context.refreshTokenRepository.tokens.get(firstLogin.getRefreshToken()).getRevokedFlag());
        assertTrue(context.refreshTokenRepository.tokens.get(secondLogin.getRefreshToken()).getRevokedFlag());
    }

    private TestContext testContext() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        UserAccountEntity user = UserAccountEntity.builder()
                .id(1L)
                .username("admin")
                .passwordHash(passwordEncoder.encode("old-password"))
                .displayName("Admin")
                .status(UserStatus.ACTIVE)
                .build();
        InMemoryUserAccountRepository userRepository = new InMemoryUserAccountRepository(user);
        InMemoryUserRoleRepository userRoleRepository = new InMemoryUserRoleRepository(UserRoleEntity.builder()
                .user(user)
                .role(RoleEntity.builder().code("ADMIN").name("Admin").build())
                .build());
        InMemoryRefreshTokenRepository refreshTokenRepository = new InMemoryRefreshTokenRepository();
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties());
        RefreshTokenService refreshTokenService = new RefreshTokenService(refreshTokenRepository.proxy(), jwtProperties());
        AuthService authService = new AuthService(
                userRepository.proxy(),
                userRoleRepository.proxy(),
                refreshTokenService,
                passwordEncoder,
                jwtTokenProvider
        );
        return new TestContext(authService, jwtTokenProvider, passwordEncoder, userRepository, refreshTokenRepository);
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-with-at-least-thirty-two-bytes");
        properties.setAccessTokenExpirationMinutes(30L);
        properties.setRefreshTokenExpirationDays(7L);
        return properties;
    }

    private record TestContext(
            AuthService authService,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder,
            InMemoryUserAccountRepository userRepository,
            InMemoryRefreshTokenRepository refreshTokenRepository
    ) {
    }

    private static class InMemoryUserAccountRepository {

        private final UserAccountEntity user;

        InMemoryUserAccountRepository(UserAccountEntity user) {
            this.user = user;
        }

        UserAccountRepository proxy() {
            return (UserAccountRepository) Proxy.newProxyInstance(
                    UserAccountRepository.class.getClassLoader(),
                    new Class<?>[]{UserAccountRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByUsername" -> user.getUsername().equals(args[0]) ? Optional.of(user) : Optional.empty();
                        case "findById" -> user.getId().equals(args[0]) ? Optional.of(user) : Optional.empty();
                        case "save" -> args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static class InMemoryUserRoleRepository {

        private final List<UserRoleEntity> userRoles;

        InMemoryUserRoleRepository(UserRoleEntity... userRoles) {
            this.userRoles = List.of(userRoles);
        }

        UserRoleRepository proxy() {
            return (UserRoleRepository) Proxy.newProxyInstance(
                    UserRoleRepository.class.getClassLoader(),
                    new Class<?>[]{UserRoleRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByUserId" -> userRoles.stream()
                                .filter(userRole -> userRole.getUser().getId().equals(args[0]))
                                .toList();
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static class InMemoryRefreshTokenRepository {

        private final Map<String, RefreshTokenEntity> tokens = new HashMap<>();

        RefreshTokenRepository proxy() {
            return (RefreshTokenRepository) Proxy.newProxyInstance(
                    RefreshTokenRepository.class.getClassLoader(),
                    new Class<?>[]{RefreshTokenRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByToken" -> Optional.ofNullable(tokens.get((String) args[0]));
                        case "findByUserId" -> findByUserId((Long) args[0]);
                        case "save" -> save((RefreshTokenEntity) args[0]);
                        case "saveAll" -> saveAll((Iterable<RefreshTokenEntity>) args[0]);
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }

        private List<RefreshTokenEntity> findByUserId(Long userId) {
            return tokens.values()
                    .stream()
                    .filter(token -> token.getUser() != null)
                    .filter(token -> userId.equals(token.getUser().getId()))
                    .toList();
        }

        private RefreshTokenEntity save(RefreshTokenEntity token) {
            tokens.put(token.getToken(), token);
            return token;
        }

        private List<RefreshTokenEntity> saveAll(Iterable<RefreshTokenEntity> values) {
            List<RefreshTokenEntity> saved = new ArrayList<>();
            values.forEach(token -> {
                tokens.put(token.getToken(), token);
                saved.add(token);
            });
            return saved;
        }
    }
}
