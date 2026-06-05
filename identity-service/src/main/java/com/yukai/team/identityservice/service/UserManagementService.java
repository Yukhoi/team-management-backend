package com.yukai.team.identityservice.service;

import com.yukai.team.identityservice.dto.request.CreateUserRequest;
import com.yukai.team.identityservice.dto.request.ResetPasswordRequest;
import com.yukai.team.identityservice.dto.request.UpdateUserRolesRequest;
import com.yukai.team.identityservice.dto.request.UpdateUserStatusRequest;
import com.yukai.team.identityservice.dto.response.PageResponse;
import com.yukai.team.identityservice.dto.response.UserManagementResponse;
import com.yukai.team.identityservice.entity.RoleEntity;
import com.yukai.team.identityservice.entity.UserAccountEntity;
import com.yukai.team.identityservice.entity.UserRoleEntity;
import com.yukai.team.identityservice.enums.UserStatus;
import com.yukai.team.identityservice.exception.BusinessException;
import com.yukai.team.identityservice.exception.ErrorCode;
import com.yukai.team.identityservice.repository.RoleRepository;
import com.yukai.team.identityservice.repository.UserAccountRepository;
import com.yukai.team.identityservice.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class UserManagementService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    public UserManagementResponse createUser(CreateUserRequest request) {
        String username = request.getUsername().trim();
        if (userAccountRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        List<RoleEntity> roles = findRoles(normalizeRoles(request.getRoles(), request.getRoleCodes()));
        UserAccountEntity user = UserAccountEntity.builder()
                .username(username)
                .displayName(request.getDisplayName().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .build();

        UserAccountEntity savedUser = userAccountRepository.save(user);
        replaceUserRoles(savedUser, roles);
        return toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserManagementResponse> getUsers(Pageable pageable) {
        return PageResponse.from(userAccountRepository.findAll(pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public UserManagementResponse getUser(Long userId) {
        return toResponse(findUser(userId));
    }

    public UserManagementResponse updateStatus(Long userId, UpdateUserStatusRequest request) {
        UserStatus status = request.getStatus();
        if (status != UserStatus.ACTIVE && status != UserStatus.DISABLED) {
            throw new BusinessException(ErrorCode.INVALID_USER_STATUS, "Only ACTIVE and DISABLED are supported");
        }

        UserAccountEntity user = findUser(userId);
        user.setStatus(status);
        UserAccountEntity savedUser = userAccountRepository.save(user);

        if (status == UserStatus.DISABLED) {
            refreshTokenService.revokeAllUserRefreshTokens(userId);
        }

        return toResponse(savedUser);
    }

    public UserManagementResponse updateRoles(Long userId, UpdateUserRolesRequest request) {
        UserAccountEntity user = findUser(userId);
        List<RoleEntity> roles = findRoles(normalizeRoles(request.getRoles(), request.getRoleCodes()));
        replaceUserRoles(user, roles);
        refreshTokenService.revokeAllUserRefreshTokens(userId);
        return toResponse(user);
    }

    public UserManagementResponse resetPassword(Long userId, ResetPasswordRequest request) {
        UserAccountEntity user = findUser(userId);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        UserAccountEntity savedUser = userAccountRepository.save(user);
        refreshTokenService.revokeAllUserRefreshTokens(userId);
        return toResponse(savedUser);
    }

    private UserAccountEntity findUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private List<RoleEntity> findRoles(Set<String> roleCodes) {
        return roleCodes.stream()
                .map(roleCode -> roleRepository.findByCode(roleCode)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND, "Role not found: " + roleCode)))
                .toList();
    }

    private Set<String> normalizeRoles(Set<String> roles, Set<String> roleCodes) {
        Set<String> source = roles != null && !roles.isEmpty() ? roles : roleCodes;
        if (source == null || source.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "roles must not be empty");
        }

        Set<String> normalizedRoles = new LinkedHashSet<>();
        for (String role : source) {
            if (!StringUtils.hasText(role)) {
                continue;
            }
            String normalizedRole = role.trim().toUpperCase();
            if (normalizedRole.startsWith("ROLE_")) {
                normalizedRole = normalizedRole.substring("ROLE_".length());
            }
            normalizedRoles.add(normalizedRole);
        }

        if (normalizedRoles.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "roles must not be empty");
        }
        return normalizedRoles;
    }

    private void replaceUserRoles(UserAccountEntity user, List<RoleEntity> roles) {
        List<UserRoleEntity> existingRoles = userRoleRepository.findByUserId(user.getId());
        userRoleRepository.deleteAll(existingRoles);

        List<UserRoleEntity> userRoles = roles.stream()
                .map(role -> UserRoleEntity.builder()
                        .user(user)
                        .role(role)
                        .build())
                .toList();
        userRoleRepository.saveAll(userRoles);
    }

    private UserManagementResponse toResponse(UserAccountEntity user) {
        List<String> roles = userRoleRepository.findByUserId(user.getId())
                .stream()
                .map(userRole -> userRole.getRole().getCode())
                .sorted(Comparator.naturalOrder())
                .toList();

        return UserManagementResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .status(user.getStatus())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
