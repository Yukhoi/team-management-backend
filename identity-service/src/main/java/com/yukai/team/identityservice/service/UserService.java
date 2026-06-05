package com.yukai.team.identityservice.service;

import com.yukai.team.identityservice.dto.request.CreateUserRequest;
import com.yukai.team.identityservice.dto.request.UpdateUserRolesRequest;
import com.yukai.team.identityservice.dto.request.UpdateUserStatusRequest;
import com.yukai.team.identityservice.dto.response.RoleResponse;
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
import com.yukai.team.identityservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public UserResponse createUser(CreateUserRequest request) {
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        List<RoleEntity> roles = findRolesByCodes(request.getRoleCodes());
        UserAccountEntity user = UserAccountEntity.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName())
                .status(UserStatus.ACTIVE)
                .build();

        UserAccountEntity savedUser = userAccountRepository.save(user);
        saveUserRoles(savedUser, roles);
        return toUserResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers() {
        return userAccountRepository.findAll()
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId) {
        UserAccountEntity user = findUser(userId);
        return toUserResponse(user);
    }

    public UserResponse updateUserStatus(Long userId, UpdateUserStatusRequest request) {
        if (SecurityUtils.isCurrentUser(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "You cannot modify your own status");
        }

        UserAccountEntity user = findUser(userId);
        user.setStatus(request.getStatus());
        UserAccountEntity savedUser = userAccountRepository.save(user);

        if (request.getStatus() == UserStatus.DISABLED || request.getStatus() == UserStatus.LOCKED) {
            refreshTokenService.revokeAllUserRefreshTokens(userId);
        }

        return toUserResponse(savedUser);
    }

    public UserResponse updateUserRoles(Long userId, UpdateUserRolesRequest request) {
        if (SecurityUtils.isCurrentUser(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "You cannot modify your own roles");
        }

        UserAccountEntity user = findUser(userId);
        List<RoleEntity> roles = findRolesByCodes(request.getRoleCodes());

        List<UserRoleEntity> existingRoles = userRoleRepository.findByUserId(userId);
        userRoleRepository.deleteAll(existingRoles);
        saveUserRoles(user, roles);
        refreshTokenService.revokeAllUserRefreshTokens(userId);

        return toUserResponse(user);
    }

    private UserAccountEntity findUser(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private List<RoleEntity> findRolesByCodes(Set<String> roleCodes) {
        return roleCodes.stream()
                .map(roleCode -> roleRepository.findByCode(roleCode)
                        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleCode)))
                .toList();
    }

    private void saveUserRoles(UserAccountEntity user, List<RoleEntity> roles) {
        List<UserRoleEntity> userRoles = roles.stream()
                .map(role -> UserRoleEntity.builder()
                        .user(user)
                        .role(role)
                        .build())
                .toList();
        userRoleRepository.saveAll(userRoles);
    }

    private UserResponse toUserResponse(UserAccountEntity user) {
        List<RoleResponse> roles = userRoleRepository.findByUserId(user.getId())
                .stream()
                .map(userRole -> RoleResponse.builder()
                        .code(userRole.getRole().getCode())
                        .name(userRole.getRole().getName())
                        .build())
                .toList();

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .status(user.getStatus())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
