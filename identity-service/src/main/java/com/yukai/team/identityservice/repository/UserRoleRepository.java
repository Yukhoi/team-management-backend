package com.yukai.team.identityservice.repository;

import com.yukai.team.identityservice.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long> {

    List<UserRoleEntity> findByUserId(Long userId);

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);
}
