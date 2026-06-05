package com.yukai.team.identityservice.service;

import com.yukai.team.identityservice.dto.response.CurrentUserResponse;
import com.yukai.team.identityservice.dto.response.RoleResponse;
import com.yukai.team.identityservice.entity.UserAccountEntity;
import com.yukai.team.identityservice.exception.BusinessException;
import com.yukai.team.identityservice.exception.ErrorCode;
import com.yukai.team.identityservice.repository.UserAccountRepository;
import com.yukai.team.identityservice.repository.UserRoleRepository;
import com.yukai.team.identityservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurrentUserService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;

    public CurrentUserResponse getCurrentUser() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        UserAccountEntity user = userAccountRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<RoleResponse> roles = userRoleRepository.findByUserId(user.getId())
                .stream()
                .map(userRole -> RoleResponse.builder()
                        .code(userRole.getRole().getCode())
                        .name(userRole.getRole().getName())
                        .build())
                .toList();

        return CurrentUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .status(user.getStatus())
                .roles(roles)
                .build();
    }
}
