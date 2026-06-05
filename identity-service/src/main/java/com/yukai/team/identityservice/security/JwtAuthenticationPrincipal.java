package com.yukai.team.identityservice.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtAuthenticationPrincipal {

    private Long userId;
    private String username;
    private List<String> roles;
}
