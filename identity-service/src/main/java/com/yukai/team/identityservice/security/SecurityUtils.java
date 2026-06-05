package com.yukai.team.identityservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Objects;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentUserId() {
        JwtAuthenticationPrincipal principal = getPrincipal();
        return principal == null ? null : principal.getUserId();
    }

    public static String getCurrentUsername() {
        JwtAuthenticationPrincipal principal = getPrincipal();
        return principal == null ? null : principal.getUsername();
    }

    public static List<String> getCurrentRoles() {
        JwtAuthenticationPrincipal principal = getPrincipal();
        return principal == null ? List.of() : principal.getRoles();
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    public static boolean isCurrentUser(Long userId) {
        Long currentUserId = getCurrentUserId();
        return currentUserId != null && Objects.equals(currentUserId, userId);
    }

    private static JwtAuthenticationPrincipal getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtAuthenticationPrincipal principal)) {
            return null;
        }
        return principal;
    }
}
