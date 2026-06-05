package com.yukai.team.identityservice.service;

import com.yukai.team.identityservice.config.JwtProperties;
import com.yukai.team.identityservice.entity.RefreshTokenEntity;
import com.yukai.team.identityservice.entity.UserAccountEntity;
import com.yukai.team.identityservice.exception.BusinessException;
import com.yukai.team.identityservice.exception.ErrorCode;
import com.yukai.team.identityservice.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenServiceTest {

    @Test
    void shouldCreateRefreshToken() {
        InMemoryRefreshTokenRepository repository = new InMemoryRefreshTokenRepository();
        RefreshTokenService refreshTokenService = new RefreshTokenService(repository.proxy(), jwtProperties());
        UserAccountEntity user = UserAccountEntity.builder().id(1L).username("admin").build();

        RefreshTokenEntity token = refreshTokenService.createRefreshToken(user);

        assertSame(user, token.getUser());
        assertFalse(token.getToken().isBlank());
        assertFalse(Boolean.TRUE.equals(token.getRevokedFlag()));
        assertTrue(token.getExpiredAt().isAfter(OffsetDateTime.now()));
        assertSame(token, repository.findSavedByToken(token.getToken()));
    }

    @Test
    void shouldValidateRefreshToken() {
        InMemoryRefreshTokenRepository repository = new InMemoryRefreshTokenRepository(validToken());
        RefreshTokenService refreshTokenService = new RefreshTokenService(repository.proxy(), jwtProperties());

        RefreshTokenEntity result = refreshTokenService.validateRefreshToken("token");

        assertEquals("token", result.getToken());
    }

    @Test
    void shouldRejectMissingRefreshToken() {
        RefreshTokenService refreshTokenService = new RefreshTokenService(
                new InMemoryRefreshTokenRepository().proxy(),
                jwtProperties()
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> refreshTokenService.validateRefreshToken("missing")
        );

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
    }

    @Test
    void shouldRejectRevokedRefreshToken() {
        RefreshTokenEntity token = validToken();
        token.setRevokedFlag(true);
        RefreshTokenService refreshTokenService = new RefreshTokenService(
                new InMemoryRefreshTokenRepository(token).proxy(),
                jwtProperties()
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> refreshTokenService.validateRefreshToken("token")
        );

        assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
    }

    @Test
    void shouldRejectExpiredRefreshToken() {
        RefreshTokenEntity token = validToken();
        token.setExpiredAt(OffsetDateTime.now().minusSeconds(1));
        RefreshTokenService refreshTokenService = new RefreshTokenService(
                new InMemoryRefreshTokenRepository(token).proxy(),
                jwtProperties()
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> refreshTokenService.validateRefreshToken("token")
        );

        assertEquals(ErrorCode.REFRESH_TOKEN_EXPIRED, exception.getErrorCode());
    }

    @Test
    void shouldRevokeRefreshToken() {
        RefreshTokenEntity token = validToken();
        RefreshTokenService refreshTokenService = new RefreshTokenService(
                new InMemoryRefreshTokenRepository(token).proxy(),
                jwtProperties()
        );

        refreshTokenService.revokeRefreshToken("token");

        assertTrue(token.getRevokedFlag());
    }

    @Test
    void shouldRevokeAllUserRefreshTokens() {
        RefreshTokenEntity first = validToken("first", 1L);
        RefreshTokenEntity second = validToken("second", 1L);
        RefreshTokenService refreshTokenService = new RefreshTokenService(
                new InMemoryRefreshTokenRepository(first, second).proxy(),
                jwtProperties()
        );

        refreshTokenService.revokeAllUserRefreshTokens(1L);

        assertTrue(first.getRevokedFlag());
        assertTrue(second.getRevokedFlag());
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setRefreshTokenExpirationDays(7L);
        return properties;
    }

    private RefreshTokenEntity validToken() {
        return validToken("token", 1L);
    }

    private RefreshTokenEntity validToken(String token, Long userId) {
        return RefreshTokenEntity.builder()
                .user(UserAccountEntity.builder().id(userId).build())
                .token(token)
                .revokedFlag(false)
                .expiredAt(OffsetDateTime.now().plusDays(1))
                .build();
    }

    private static class InMemoryRefreshTokenRepository {

        private final Map<String, RefreshTokenEntity> tokens = new HashMap<>();

        InMemoryRefreshTokenRepository(RefreshTokenEntity... initialTokens) {
            for (RefreshTokenEntity token : initialTokens) {
                tokens.put(token.getToken(), token);
            }
        }

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

        RefreshTokenEntity findSavedByToken(String token) {
            return tokens.get(token);
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
