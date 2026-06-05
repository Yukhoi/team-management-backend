package com.yukai.team.identityservice.controller;

import com.yukai.team.identityservice.common.ApiResponse;
import com.yukai.team.identityservice.dto.request.ChangePasswordRequest;
import com.yukai.team.identityservice.dto.request.LoginRequest;
import com.yukai.team.identityservice.dto.request.LogoutRequest;
import com.yukai.team.identityservice.dto.request.RefreshTokenRequest;
import com.yukai.team.identityservice.dto.response.CurrentUserResponse;
import com.yukai.team.identityservice.dto.response.LoginResponse;
import com.yukai.team.identityservice.dto.response.TokenRefreshResponse;
import com.yukai.team.identityservice.enums.UserStatus;
import com.yukai.team.identityservice.exception.BusinessException;
import com.yukai.team.identityservice.exception.ErrorCode;
import com.yukai.team.identityservice.security.JwtAuthenticationPrincipal;
import com.yukai.team.identityservice.service.AuthService;
import com.yukai.team.identityservice.service.CurrentUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthControllerTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldLogin() {
        AuthController controller = new AuthController(authService(), currentUserService());

        ApiResponse<LoginResponse> response = controller.login(LoginRequest.builder()
                .username("admin")
                .password("123456")
                .build());

        assertEquals(true, response.getSuccess());
        assertEquals("Login successful", response.getMessage());
        assertEquals("access-token", response.getData().getAccessToken());
        assertEquals("refresh-token", response.getData().getRefreshToken());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void shouldRefreshToken() {
        AuthController controller = new AuthController(authService(), currentUserService());

        ApiResponse<TokenRefreshResponse> response = controller.refresh(RefreshTokenRequest.builder()
                .refreshToken("refresh-token")
                .build());

        assertEquals(true, response.getSuccess());
        assertEquals("Token refreshed successfully", response.getMessage());
        assertEquals("new-access-token", response.getData().getAccessToken());
    }

    @Test
    void shouldLogout() {
        AuthController controller = new AuthController(authService(), currentUserService());

        ApiResponse<Void> response = controller.logout(LogoutRequest.builder()
                .refreshToken("refresh-token")
                .build());

        assertEquals(true, response.getSuccess());
        assertEquals("Logout successful", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void shouldChangePasswordForCurrentUser() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                JwtAuthenticationPrincipal.builder().userId(1L).username("admin").roles(List.of("ROLE_ADMIN")).build(),
                null,
                List.of()
        ));
        AuthController controller = new AuthController(authService(), currentUserService());

        ApiResponse<Void> response = controller.changePassword(ChangePasswordRequest.builder()
                .oldPassword("123456")
                .newPassword("654321")
                .build());

        assertEquals(true, response.getSuccess());
        assertEquals("Password changed successfully", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void shouldRejectChangePasswordWithoutCurrentUser() {
        AuthController controller = new AuthController(authService(), currentUserService());

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> controller.changePassword(ChangePasswordRequest.builder()
                        .oldPassword("123456")
                        .newPassword("654321")
                        .build())
        );

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void shouldReturnCurrentUser() {
        AuthController controller = new AuthController(authService(), currentUserService());

        ApiResponse<CurrentUserResponse> response = controller.me();

        assertEquals(true, response.getSuccess());
        assertEquals("Success", response.getMessage());
        assertEquals("admin", response.getData().getUsername());
    }

    private AuthService authService() {
        return new TestAuthService();
    }

    private CurrentUserService currentUserService() {
        return new TestCurrentUserService();
    }

    private static class TestAuthService extends AuthService {

        TestAuthService() {
            super(null, null, null, null, null);
        }

        @Override
        public LoginResponse login(LoginRequest request) {
            return LoginResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(1800L)
                    .build();
        }

        @Override
        public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
            return TokenRefreshResponse.builder()
                    .accessToken("new-access-token")
                    .tokenType("Bearer")
                    .expiresIn(1800L)
                    .build();
        }

        @Override
        public void logout(LogoutRequest request) {
        }

        @Override
        public void changePassword(Long userId, ChangePasswordRequest request) {
        }
    }

    private static class TestCurrentUserService extends CurrentUserService {

        TestCurrentUserService() {
            super(null, null);
        }

        @Override
        public CurrentUserResponse getCurrentUser() {
            return CurrentUserResponse.builder()
                    .id(1L)
                    .username("admin")
                    .displayName("Admin")
                    .status(UserStatus.ACTIVE)
                    .roles(List.of())
                    .build();
        }
    }
}
