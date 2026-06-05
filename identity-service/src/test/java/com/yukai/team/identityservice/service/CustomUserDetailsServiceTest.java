package com.yukai.team.identityservice.service;

import com.yukai.team.identityservice.entity.RoleEntity;
import com.yukai.team.identityservice.entity.UserAccountEntity;
import com.yukai.team.identityservice.entity.UserRoleEntity;
import com.yukai.team.identityservice.enums.UserStatus;
import com.yukai.team.identityservice.repository.UserAccountRepository;
import com.yukai.team.identityservice.repository.UserRoleRepository;
import com.yukai.team.identityservice.security.IdentityUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomUserDetailsServiceTest {

    @Test
    void shouldLoadUserDetailsWithRoleAuthorities() {
        UserAccountEntity user = UserAccountEntity.builder()
                .id(1L)
                .username("admin")
                .passwordHash("hash")
                .displayName("Admin")
                .status(UserStatus.ACTIVE)
                .build();
        UserRoleEntity userRole = UserRoleEntity.builder()
                .user(user)
                .role(RoleEntity.builder().code("ADMIN").name("Admin").build())
                .build();
        CustomUserDetailsService service = new CustomUserDetailsService(
                userAccountRepository(user),
                userRoleRepository(userRole)
        );

        UserDetails userDetails = service.loadUserByUsername("admin");

        IdentityUserDetails identityUserDetails = (IdentityUserDetails) userDetails;
        assertEquals(1L, identityUserDetails.getId());
        assertEquals("admin", identityUserDetails.getUsername());
        assertEquals("hash", identityUserDetails.getPassword());
        assertEquals("Admin", identityUserDetails.getDisplayName());
        assertEquals(List.of("ROLE_ADMIN"), identityUserDetails.getAuthorities()
                .stream()
                .map(Object::toString)
                .toList());
    }

    private UserAccountRepository userAccountRepository(UserAccountEntity user) {
        return (UserAccountRepository) Proxy.newProxyInstance(
                UserAccountRepository.class.getClassLoader(),
                new Class<?>[]{UserAccountRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByUsername" -> user.getUsername().equals(args[0]) ? Optional.of(user) : Optional.empty();
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private UserRoleRepository userRoleRepository(UserRoleEntity userRole) {
        return (UserRoleRepository) Proxy.newProxyInstance(
                UserRoleRepository.class.getClassLoader(),
                new Class<?>[]{UserRoleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByUserId" -> userRole.getUser().getId().equals(args[0]) ? List.of(userRole) : List.of();
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
