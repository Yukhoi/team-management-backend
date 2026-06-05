package com.yukai.team.identityservice.service;

import com.yukai.team.identityservice.config.JwtProperties;
import com.yukai.team.identityservice.entity.RefreshTokenEntity;
import com.yukai.team.identityservice.entity.UserAccountEntity;
import com.yukai.team.identityservice.exception.BusinessException;
import com.yukai.team.identityservice.exception.ErrorCode;
import com.yukai.team.identityservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Transactional
    public RefreshTokenEntity createRefreshToken(UserAccountEntity user) {
        RefreshTokenEntity refreshToken = RefreshTokenEntity.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiredAt(OffsetDateTime.now().plusDays(jwtProperties.getRefreshTokenExpirationDays()))
                .revokedFlag(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshTokenEntity validateRefreshToken(String refreshToken) {
        RefreshTokenEntity token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("Refresh token validation failed: token not found");
                    return new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
                });

        if (Boolean.TRUE.equals(token.getRevokedFlag())) {
            log.warn("Refresh token validation failed: token revoked");
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        if (OffsetDateTime.now().isAfter(token.getExpiredAt())) {
            log.warn("Refresh token validation failed: token expired");
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        return token;
    }

    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(token -> {
                    token.setRevokedFlag(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Transactional
    public void revokeAllUserRefreshTokens(Long userId) {
        List<RefreshTokenEntity> tokens = refreshTokenRepository.findByUserId(userId);
        tokens.forEach(token -> token.setRevokedFlag(true));
        refreshTokenRepository.saveAll(tokens);
    }
}
