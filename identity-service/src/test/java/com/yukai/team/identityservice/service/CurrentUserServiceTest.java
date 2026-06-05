package com.yukai.team.identityservice.service;

import com.yukai.team.identityservice.dto.response.CurrentUserResponse;
import com.yukai.team.identityservice.entity.RoleEntity;
import com.yukai.team.identityservice.entity.UserAccountEntity;
import com.yukai.team.identityservice.entity.UserRoleEntity;
import com.yukai.team.identityservice.enums.UserStatus;
import com.yukai.team.identityservice.exception.BusinessException;
import com.yukai.team.identityservice.exception.ErrorCode;
import com.yukai.team.identityservice.repository.UserAccountRepository;
import com.yukai.team.identityservice.repository.UserRoleRepository;
import com.yukai.team.identityservice.security.JwtAuthenticationPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrentUserServiceTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentUser() {
        UserAccountEntity user = UserAccountEntity.builder()
                .id(1L)
                .username("admin")
                .displayName("Admin")
                .status(UserStatus.ACTIVE)
                .build();
        UserRoleEntity userRole = UserRoleEntity.builder()
                .user(user)
                .role(RoleEntity.builder().code("ADMIN").name("Administrator").build())
                .build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                JwtAuthenticationPrincipal.builder()
                        .userId(1L)
                        .username("admin")
                        .roles(List.of("ROLE_ADMIN"))
                        .build(),
                null,
                List.of()
        ));

        CurrentUserService service = new CurrentUserService(
                userAccountRepository(user),
                userRoleRepository(userRole)
        );

        CurrentUserResponse response = service.getCurrentUser();

        assertEquals(1L, response.getId());
        assertEquals("admin", response.getUsername());
        assertEquals("Admin", response.getDisplayName());
        assertEquals(UserStatus.ACTIVE, response.getStatus());
        assertEquals("ADMIN", response.getRoles().get(0).getCode());
        assertEquals("Administrator", response.getRoles().get(0).getName());
    }

    @Test
    void shouldRejectWhenSecurityContextHasNoCurrentUser() {
        CurrentUserService service = new CurrentUserService(
                userAccountRepository(null),
                userRoleRepository()
        );

        BusinessException exception = assertThrows(BusinessException.class, service::getCurrentUser);

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
    }

    private UserAccountRepository userAccountRepository(UserAccountEntity user) {
        return (UserAccountRepository) Proxy.newProxyInstance(
                UserAccountRepository.class.getClassLoader(),
                new Class<?>[]{UserAccountRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> user != null && user.getId().equals(args[0]) ? Optional.of(user) : Optional.empty();
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private UserRoleRepository userRoleRepository(UserRoleEntity... roles) {
        return (UserRoleRepository) Proxy.newProxyInstance(
                UserRoleRepository.class.getClassLoader(),
                new Class<?>[]{UserRoleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByUserId" -> List.of(roles)
                            .stream()
                            .filter(role -> role.getUser().getId().equals(args[0]))
                            .toList();
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
