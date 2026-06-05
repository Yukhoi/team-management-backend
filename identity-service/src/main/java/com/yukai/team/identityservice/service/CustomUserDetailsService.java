package com.yukai.team.identityservice.service;

import com.yukai.team.identityservice.entity.UserAccountEntity;
import com.yukai.team.identityservice.repository.UserAccountRepository;
import com.yukai.team.identityservice.repository.UserRoleRepository;
import com.yukai.team.identityservice.security.IdentityUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccountEntity user = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User details load failed: username not found, username={}", username);
                    return new UsernameNotFoundException("User not found");
                });

        return IdentityUserDetails.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .displayName(user.getDisplayName())
                .status(user.getStatus())
                .authorities(userRoleRepository.findByUserId(user.getId())
                        .stream()
                        .map(userRole -> userRole.getRole().getCode())
                        .map(this::toRoleAuthority)
                        .map(SimpleGrantedAuthority::new)
                        .toList())
                .build();
    }

    private String toRoleAuthority(String roleCode) {
        if (roleCode.startsWith("ROLE_")) {
            return roleCode;
        }
        return "ROLE_" + roleCode;
    }
}
