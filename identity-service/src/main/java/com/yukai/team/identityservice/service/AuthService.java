package com.yukai.team.identityservice.service;

import com.yukai.team.identityservice.dto.request.ChangePasswordRequest;
import com.yukai.team.identityservice.dto.request.LoginRequest;
import com.yukai.team.identityservice.dto.request.LogoutRequest;
import com.yukai.team.identityservice.dto.request.RefreshTokenRequest;
import com.yukai.team.identityservice.dto.response.LoginResponse;
import com.yukai.team.identityservice.dto.response.TokenRefreshResponse;
import com.yukai.team.identityservice.entity.RefreshTokenEntity;
import com.yukai.team.identityservice.entity.UserAccountEntity;
import com.yukai.team.identityservice.enums.UserStatus;
import com.yukai.team.identityservice.exception.BusinessException;
import com.yukai.team.identityservice.exception.ErrorCode;
import com.yukai.team.identityservice.repository.UserAccountRepository;
import com.yukai.team.identityservice.repository.UserRoleRepository;
import com.yukai.team.identityservice.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse login(LoginRequest request) {
        UserAccountEntity user = userAccountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found, username={}", request.getUsername());
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        validateUserStatus(user);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: invalid credentials, username={}", user.getUsername());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        List<String> roles = getUserRoles(user.getId());
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), roles);
        RefreshTokenEntity refreshToken = refreshTokenService.createRefreshToken(user);

        user.setLastLoginAt(OffsetDateTime.now());
        userAccountRepository.save(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationSeconds())
                .build();
    }

    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        RefreshTokenEntity refreshToken = refreshTokenService.validateRefreshToken(request.getRefreshToken());
        UserAccountEntity user = refreshToken.getUser();

        validateUserStatus(user);

        List<String> roles = getUserRoles(user.getId());
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), roles);

        return TokenRefreshResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationSeconds())
                .build();
    }

    public void logout(LogoutRequest request) {
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {
        UserAccountEntity user = userAccountRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Change password failed: user not found, userId={}", userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            log.warn("Change password failed: invalid credentials, userId={}", userId);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userAccountRepository.save(user);
        refreshTokenService.revokeAllUserRefreshTokens(userId);
        log.warn("Password changed and refresh tokens revoked, userId={}", userId);
    }

    private void validateUserStatus(UserAccountEntity user) {
        if (user.getStatus() == UserStatus.DISABLED) {
            log.warn("Authentication rejected: user disabled, username={}", user.getUsername());
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }
        if (user.getStatus() == UserStatus.LOCKED) {
            log.warn("Authentication rejected: user locked, username={}", user.getUsername());
            throw new BusinessException(ErrorCode.USER_LOCKED);
        }
    }

    private List<String> getUserRoles(Long userId) {
        return userRoleRepository.findByUserId(userId)
                .stream()
                .map(userRole -> userRole.getRole().getCode())
                .map(this::toRoleAuthority)
                .toList();
    }

    private String toRoleAuthority(String roleCode) {
        if (roleCode.startsWith("ROLE_")) {
            return roleCode;
        }
        return "ROLE_" + roleCode;
    }
}
