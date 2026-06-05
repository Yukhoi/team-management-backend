package com.yukai.team.identityservice.service;

import com.yukai.team.identityservice.dto.request.CreateUserRequest;
import com.yukai.team.identityservice.dto.request.UpdateUserRolesRequest;
import com.yukai.team.identityservice.dto.request.UpdateUserStatusRequest;
import com.yukai.team.identityservice.dto.response.UserResponse;
import com.yukai.team.identityservice.entity.RoleEntity;
import com.yukai.team.identityservice.entity.UserAccountEntity;
import com.yukai.team.identityservice.entity.UserRoleEntity;
import com.yukai.team.identityservice.enums.UserStatus;
import com.yukai.team.identityservice.exception.BusinessException;
import com.yukai.team.identityservice.exception.ErrorCode;
import com.yukai.team.identityservice.repository.RoleRepository;
import com.yukai.team.identityservice.repository.UserAccountRepository;
import com.yukai.team.identityservice.repository.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.yukai.team.identityservice.security.JwtAuthenticationPrincipal;

import java.lang.reflect.Proxy;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreateUser() {
        TestContext context = testContext();

        UserResponse response = context.userService.createUser(CreateUserRequest.builder()
                .username("coach1")
                .password("123456")
                .displayName("Coach User")
                .roleCodes(new LinkedHashSet<>(Set.of("COACH")))
                .build());

        assertEquals("coach1", response.getUsername());
        assertEquals("Coach User", response.getDisplayName());
        assertEquals(UserStatus.ACTIVE, response.getStatus());
        assertEquals("COACH", response.getRoles().get(0).getCode());
        assertTrue(context.passwordEncoder.matches("123456", context.userRepository.users.get(1L).getPasswordHash()));
    }

    @Test
    void shouldRejectDuplicateUsername() {
        TestContext context = testContext();
        context.userRepository.save(existingUser());

        BusinessException exception = assertThrows(BusinessException.class, () -> context.userService.createUser(
                CreateUserRequest.builder()
                        .username("admin")
                        .password("123456")
                        .displayName("Admin")
                        .roleCodes(Set.of("ADMIN"))
                        .build()
        ));

        assertEquals(ErrorCode.USERNAME_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    void shouldGetUsers() {
        TestContext context = testContext();
        UserAccountEntity user = context.userRepository.save(existingUser());
        context.userRoleRepository.saveAll(List.of(userRole(user, context.roleRepository.roles.get("ADMIN"))));

        List<UserResponse> users = context.userService.getUsers();

        assertEquals(1, users.size());
        assertEquals("admin", users.get(0).getUsername());
        assertEquals("ADMIN", users.get(0).getRoles().get(0).getCode());
    }

    @Test
    void shouldGetUserById() {
        TestContext context = testContext();
        UserAccountEntity user = context.userRepository.save(existingUser());
        context.userRoleRepository.saveAll(List.of(userRole(user, context.roleRepository.roles.get("ADMIN"))));

        UserResponse response = context.userService.getUserById(user.getId());

        assertEquals("admin", response.getUsername());
        assertEquals("ADMIN", response.getRoles().get(0).getCode());
    }

    @Test
    void shouldUpdateUserStatusAndRevokeTokensWhenDisabled() {
        TestContext context = testContext();
        UserAccountEntity user = context.userRepository.save(existingUser());

        UserResponse response = context.userService.updateUserStatus(user.getId(), UpdateUserStatusRequest.builder()
                .status(UserStatus.DISABLED)
                .build());

        assertEquals(UserStatus.DISABLED, response.getStatus());
        assertEquals(List.of(user.getId()), context.refreshTokenService.revokedUserIds);
    }

    @Test
    void shouldRejectUpdatingOwnStatus() {
        TestContext context = testContext();
        UserAccountEntity user = context.userRepository.save(existingUser());
        authenticateAs(user.getId());

        BusinessException exception = assertThrows(BusinessException.class, () -> context.userService.updateUserStatus(
                user.getId(),
                UpdateUserStatusRequest.builder()
                        .status(UserStatus.DISABLED)
                        .build()
        ));

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        assertEquals("You cannot modify your own status", exception.getMessage());
    }

    @Test
    void shouldUpdateUserRolesAndRevokeTokens() {
        TestContext context = testContext();
        UserAccountEntity user = context.userRepository.save(existingUser());
        context.userRoleRepository.saveAll(List.of(userRole(user, context.roleRepository.roles.get("ADMIN"))));

        UserResponse response = context.userService.updateUserRoles(user.getId(), UpdateUserRolesRequest.builder()
                .roleCodes(Set.of("COACH"))
                .build());

        assertEquals(1, response.getRoles().size());
        assertEquals("COACH", response.getRoles().get(0).getCode());
        assertEquals(List.of(user.getId()), context.refreshTokenService.revokedUserIds);
    }

    @Test
    void shouldRejectUpdatingOwnRoles() {
        TestContext context = testContext();
        UserAccountEntity user = context.userRepository.save(existingUser());
        authenticateAs(user.getId());

        BusinessException exception = assertThrows(BusinessException.class, () -> context.userService.updateUserRoles(
                user.getId(),
                UpdateUserRolesRequest.builder()
                        .roleCodes(Set.of("COACH"))
                        .build()
        ));

        assertEquals(ErrorCode.ACCESS_DENIED, exception.getErrorCode());
        assertEquals("You cannot modify your own roles", exception.getMessage());
    }

    @Test
    void shouldRejectMissingRole() {
        TestContext context = testContext();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> context.userService.createUser(
                CreateUserRequest.builder()
                        .username("missing")
                        .password("123456")
                        .displayName("Missing")
                        .roleCodes(Set.of("MISSING"))
                        .build()
        ));

        assertEquals("Role not found: MISSING", exception.getMessage());
    }

    private TestContext testContext() {
        InMemoryUserAccountRepository userRepository = new InMemoryUserAccountRepository();
        InMemoryRoleRepository roleRepository = new InMemoryRoleRepository(List.of(
                RoleEntity.builder().id(1L).code("ADMIN").name("Admin").build(),
                RoleEntity.builder().id(2L).code("COACH").name("Coach").build()
        ));
        InMemoryUserRoleRepository userRoleRepository = new InMemoryUserRoleRepository();
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        TestRefreshTokenService refreshTokenService = new TestRefreshTokenService();
        UserService userService = new UserService(
                userRepository.proxy(),
                roleRepository.proxy(),
                userRoleRepository.proxy(),
                passwordEncoder,
                refreshTokenService
        );
        return new TestContext(userService, userRepository, roleRepository, userRoleRepository, passwordEncoder, refreshTokenService);
    }

    private UserAccountEntity existingUser() {
        return UserAccountEntity.builder()
                .username("admin")
                .passwordHash("hash")
                .displayName("Admin")
                .status(UserStatus.ACTIVE)
                .build();
    }

    private UserRoleEntity userRole(UserAccountEntity user, RoleEntity role) {
        return UserRoleEntity.builder()
                .user(user)
                .role(role)
                .build();
    }

    private void authenticateAs(Long userId) {
        JwtAuthenticationPrincipal principal = JwtAuthenticationPrincipal.builder()
                .userId(userId)
                .username("admin")
                .roles(List.of("ROLE_ADMIN"))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    private record TestContext(
            UserService userService,
            InMemoryUserAccountRepository userRepository,
            InMemoryRoleRepository roleRepository,
            InMemoryUserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            TestRefreshTokenService refreshTokenService
    ) {
    }

    private static class TestRefreshTokenService extends RefreshTokenService {

        private final List<Long> revokedUserIds = new ArrayList<>();

        TestRefreshTokenService() {
            super(null, null);
        }

        @Override
        public void revokeAllUserRefreshTokens(Long userId) {
            revokedUserIds.add(userId);
        }
    }

    private static class InMemoryUserAccountRepository {

        private final Map<Long, UserAccountEntity> users = new java.util.LinkedHashMap<>();
        private final AtomicLong idSequence = new AtomicLong(1);

        UserAccountRepository proxy() {
            return (UserAccountRepository) Proxy.newProxyInstance(
                    UserAccountRepository.class.getClassLoader(),
                    new Class<?>[]{UserAccountRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "existsByUsername" -> users.values().stream()
                                .anyMatch(user -> user.getUsername().equals(args[0]));
                        case "save" -> save((UserAccountEntity) args[0]);
                        case "findAll" -> new ArrayList<>(users.values());
                        case "findById" -> Optional.ofNullable(users.get((Long) args[0]));
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }

        UserAccountEntity save(UserAccountEntity user) {
            if (user.getId() == null) {
                user.setId(idSequence.getAndIncrement());
            }
            if (user.getCreatedAt() == null) {
                user.setCreatedAt(OffsetDateTime.now());
            }
            user.setUpdatedAt(OffsetDateTime.now());
            users.put(user.getId(), user);
            return user;
        }
    }

    private static class InMemoryRoleRepository {

        private final Map<String, RoleEntity> roles = new java.util.HashMap<>();

        InMemoryRoleRepository(List<RoleEntity> roles) {
            roles.forEach(role -> this.roles.put(role.getCode(), role));
        }

        RoleRepository proxy() {
            return (RoleRepository) Proxy.newProxyInstance(
                    RoleRepository.class.getClassLoader(),
                    new Class<?>[]{RoleRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByCode" -> Optional.ofNullable(roles.get((String) args[0]));
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static class InMemoryUserRoleRepository {

        private final List<UserRoleEntity> userRoles = new ArrayList<>();

        UserRoleRepository proxy() {
            return (UserRoleRepository) Proxy.newProxyInstance(
                    UserRoleRepository.class.getClassLoader(),
                    new Class<?>[]{UserRoleRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByUserId" -> findByUserId((Long) args[0]);
                        case "saveAll" -> saveAll((Iterable<UserRoleEntity>) args[0]);
                        case "deleteAll" -> {
                            deleteAll((Iterable<UserRoleEntity>) args[0]);
                            yield null;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }

        private List<UserRoleEntity> findByUserId(Long userId) {
            return userRoles.stream()
                    .filter(userRole -> userRole.getUser().getId().equals(userId))
                    .toList();
        }

        private List<UserRoleEntity> saveAll(Iterable<UserRoleEntity> values) {
            List<UserRoleEntity> saved = new ArrayList<>();
            values.forEach(userRole -> {
                userRoles.add(userRole);
                saved.add(userRole);
            });
            return saved;
        }

        private void deleteAll(Iterable<UserRoleEntity> values) {
            values.forEach(userRoles::remove);
        }
    }
}
