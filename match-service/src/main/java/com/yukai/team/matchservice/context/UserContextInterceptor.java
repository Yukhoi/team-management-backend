package com.yukai.team.matchservice.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
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
        CurrentUser currentUser = buildCurrentUser(request);
        if (currentUser != null) {
            UserContextHolder.set(currentUser);
        }
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        UserContextHolder.clear();
    }

    private CurrentUser buildCurrentUser(HttpServletRequest request) {
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        String username = request.getHeader(USERNAME_HEADER);
        String rolesHeader = request.getHeader(USER_ROLES_HEADER);

        if (isBlank(userIdHeader) && isBlank(username) && isBlank(rolesHeader)) {
            return null;
        }

        return new CurrentUser(
                parseUserId(userIdHeader),
                blankToNull(username),
                parseRoles(rolesHeader)
        );
    }

    private Long parseUserId(String value) {
        if (isBlank(value)) {
            return null;
        }

        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private List<String> parseRoles(String value) {
        if (isBlank(value)) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .toList();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
