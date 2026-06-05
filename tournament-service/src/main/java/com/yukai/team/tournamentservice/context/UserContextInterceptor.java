package com.yukai.team.tournamentservice.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContextHolder.clear();

        String userIdHeader = request.getHeader(USER_ID_HEADER);
        String username = request.getHeader(USERNAME_HEADER);
        String rolesHeader = request.getHeader(USER_ROLES_HEADER);

        if (!StringUtils.hasText(userIdHeader) && !StringUtils.hasText(username) && !StringUtils.hasText(rolesHeader)) {
            return true;
        }

        Long userId = parseUserId(userIdHeader);
        List<String> roles = parseRoles(rolesHeader);
        UserContextHolder.set(new CurrentUser(userId, username, roles));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }

    private Long parseUserId(String userIdHeader) {
        if (!StringUtils.hasText(userIdHeader)) {
            return null;
        }
        try {
            return Long.valueOf(userIdHeader);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<String> parseRoles(String rolesHeader) {
        if (!StringUtils.hasText(rolesHeader)) {
            return List.of();
        }
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }
}
