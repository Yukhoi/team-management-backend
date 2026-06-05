package com.yukai.team.identityservice.config;

import com.yukai.team.identityservice.entity.RoleEntity;
import com.yukai.team.identityservice.entity.UserAccountEntity;
import com.yukai.team.identityservice.entity.UserRoleEntity;
import com.yukai.team.identityservice.enums.UserStatus;
import com.yukai.team.identityservice.repository.RoleRepository;
import com.yukai.team.identityservice.repository.UserAccountRepository;
import com.yukai.team.identityservice.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class DataInitializer implements ApplicationRunner {

    private static final String ADMIN_ROLE_CODE = "ADMIN";
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";

    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final InitProperties initProperties;

    @Override
    public void run(ApplicationArguments args) {
        initializeRoles();
        initializeAdmin();
    }

    private void initializeRoles() {
        List<RoleDefinition> roles = List.of(
                new RoleDefinition(ADMIN_ROLE_CODE, "Administrator", "System administrator"),
                new RoleDefinition("COACH", "Coach", "Team coach"),
                new RoleDefinition("PLAYER", "Player", "Team player")
        );

        for (RoleDefinition role : roles) {
            if (roleRepository.findByCode(role.code()).isPresent()) {
                log.info("role initialization skipped: {}", role.code());
                continue;
            }

            roleRepository.save(RoleEntity.builder()
                    .code(role.code())
                    .name(role.name())
                    .description(role.description())
                    .build());
            log.info("role created: {}", role.code());
        }

        log.info("role initialized");
    }

    private void initializeAdmin() {
        RoleEntity adminRole = roleRepository.findByCode(ADMIN_ROLE_CODE)
                .orElseThrow(() -> new IllegalStateException("ADMIN role must exist before admin initialization"));

        UserAccountEntity admin = userAccountRepository.findByUsername(initProperties.getAdminUsername())
                .map(existingAdmin -> {
                    log.info("admin initialization skipped: {}", existingAdmin.getUsername());
                    return existingAdmin;
                })
                .orElseGet(() -> createAdminUser());

        if (userRoleRepository.existsByUserIdAndRoleId(admin.getId(), adminRole.getId())) {
            log.info("admin role initialization skipped: {}", admin.getUsername());
            log.info("admin initialized");
            return;
        }

        userRoleRepository.save(UserRoleEntity.builder()
                .user(admin)
                .role(adminRole)
                .build());
        log.info("admin role initialized: {}", admin.getUsername());
        log.info("admin initialized");
    }

    private UserAccountEntity createAdminUser() {
        UserAccountEntity admin = userAccountRepository.save(UserAccountEntity.builder()
                .username(initProperties.getAdminUsername())
                .passwordHash(passwordEncoder.encode(initProperties.getAdminPassword()))
                .displayName(initProperties.getAdminDisplayName())
                .status(UserStatus.ACTIVE)
                .build());

        log.info("Default admin user created: {}", admin.getUsername());
        if (DEFAULT_ADMIN_PASSWORD.equals(initProperties.getAdminPassword())) {
            log.warn("Using default admin password. Please change it immediately.");
        }

        return admin;
    }

    private record RoleDefinition(String code, String name, String description) {
    }
}
