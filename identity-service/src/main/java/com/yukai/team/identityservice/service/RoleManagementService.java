package com.yukai.team.identityservice.service;

import com.yukai.team.identityservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleManagementService {

    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<String> getRoles() {
        return roleRepository.findAll()
                .stream()
                .map(role -> role.getCode())
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
