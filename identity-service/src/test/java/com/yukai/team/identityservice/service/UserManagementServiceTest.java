package com.yukai.team.identityservice.service;

import com.yukai.team.identityservice.dto.request.UpdateUserRolesRequest;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    private static final Long USER_ID = 3L;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserManagementService userManagementService;

    @Test
    @DisplayName("adds only missing role when request includes an existing role")
    void shouldOnlyAddMissingRoleWhenRequestedRolesContainExistingRole() {
        UserAccountEntity user = user();
        RoleEntity coach = role(2L, "COACH");
        RoleEntity player = role(4L, "PLAYER");
        UserRoleEntity existingCoachRelation = userRole(user, coach);

        when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("COACH")).thenReturn(Optional.of(coach));
        when(roleRepository.findByCode("PLAYER")).thenReturn(Optional.of(player));
        when(userRoleRepository.findByUserId(USER_ID))
                .thenReturn(List.of(existingCoachRelation))
                .thenReturn(List.of(existingCoachRelation, userRole(user, player)));

        UserManagementResponse response = userManagementService.updateRoles(
                USER_ID,
                request("COACH", "PLAYER")
        );

        verify(userRoleRepository, never()).deleteAll(any());
        ArgumentCaptor<Iterable<UserRoleEntity>> saveAllCaptor = userRoleIterableCaptor();
        verify(userRoleRepository).saveAll(saveAllCaptor.capture());

        List<UserRoleEntity> savedRelations = toList(saveAllCaptor.getValue());
        assertThat(savedRelations).hasSize(1);
        assertThat(savedRelations.get(0).getUser()).isSameAs(user);
        assertThat(savedRelations.get(0).getRole()).isSameAs(player);

        verify(refreshTokenService).revokeAllUserRefreshTokens(USER_ID);
        assertThat(response.getRoles()).containsExactlyInAnyOrder("COACH", "PLAYER");
    }

    @Test
    @DisplayName("does not modify relations when requested roles are unchanged")
    void shouldNotModifyRelationsWhenRequestedRolesAreUnchanged() {
        UserAccountEntity user = user();
        RoleEntity coach = role(2L, "COACH");
        RoleEntity player = role(4L, "PLAYER");
        List<UserRoleEntity> existingRelations = List.of(userRole(user, coach), userRole(user, player));

        when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("COACH")).thenReturn(Optional.of(coach));
        when(roleRepository.findByCode("PLAYER")).thenReturn(Optional.of(player));
        when(userRoleRepository.findByUserId(USER_ID)).thenReturn(existingRelations);

        UserManagementResponse response = userManagementService.updateRoles(
                USER_ID,
                request("COACH", "PLAYER")
        );

        verify(userRoleRepository, never()).deleteAll(any());
        verify(userRoleRepository, never()).saveAll(any());
        verify(refreshTokenService).revokeAllUserRefreshTokens(USER_ID);
        assertThat(response.getRoles()).containsExactlyInAnyOrder("COACH", "PLAYER");
    }

    @Test
    @DisplayName("deletes only removed roles")
    void shouldDeleteOnlyRemovedRoles() {
        UserAccountEntity user = user();
        RoleEntity admin = role(1L, "ADMIN");
        RoleEntity coach = role(2L, "COACH");
        UserRoleEntity adminRelation = userRole(user, admin);
        UserRoleEntity coachRelation = userRole(user, coach);

        when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("ADMIN")).thenReturn(Optional.of(admin));
        when(userRoleRepository.findByUserId(USER_ID))
                .thenReturn(List.of(adminRelation, coachRelation))
                .thenReturn(List.of(adminRelation));

        UserManagementResponse response = userManagementService.updateRoles(
                USER_ID,
                request("ADMIN")
        );

        ArgumentCaptor<Iterable<UserRoleEntity>> deleteAllCaptor = userRoleIterableCaptor();
        verify(userRoleRepository).deleteAll(deleteAllCaptor.capture());
        assertThat(toList(deleteAllCaptor.getValue())).containsExactly(coachRelation);

        verify(userRoleRepository, never()).saveAll(any());
        verify(refreshTokenService).revokeAllUserRefreshTokens(USER_ID);
        assertThat(response.getRoles()).containsExactly("ADMIN");
    }

    @Test
    @DisplayName("deletes old roles and adds new role")
    void shouldDeleteOldRolesAndAddNewRole() {
        UserAccountEntity user = user();
        RoleEntity admin = role(1L, "ADMIN");
        RoleEntity coach = role(2L, "COACH");
        RoleEntity player = role(4L, "PLAYER");
        UserRoleEntity adminRelation = userRole(user, admin);
        UserRoleEntity coachRelation = userRole(user, coach);

        when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("PLAYER")).thenReturn(Optional.of(player));
        when(userRoleRepository.findByUserId(USER_ID))
                .thenReturn(List.of(adminRelation, coachRelation))
                .thenReturn(List.of(userRole(user, player)));

        UserManagementResponse response = userManagementService.updateRoles(
                USER_ID,
                request("PLAYER")
        );

        ArgumentCaptor<Iterable<UserRoleEntity>> deleteAllCaptor = userRoleIterableCaptor();
        verify(userRoleRepository).deleteAll(deleteAllCaptor.capture());
        assertThat(toList(deleteAllCaptor.getValue()))
                .containsExactlyInAnyOrder(adminRelation, coachRelation);

        ArgumentCaptor<Iterable<UserRoleEntity>> saveAllCaptor = userRoleIterableCaptor();
        verify(userRoleRepository).saveAll(saveAllCaptor.capture());
        List<UserRoleEntity> savedRelations = toList(saveAllCaptor.getValue());
        assertThat(savedRelations).hasSize(1);
        assertThat(savedRelations.get(0).getUser()).isSameAs(user);
        assertThat(savedRelations.get(0).getRole()).isSameAs(player);

        verify(userRoleRepository, times(1)).deleteAll(any());
        verify(userRoleRepository, times(1)).saveAll(any());
        verify(refreshTokenService).revokeAllUserRefreshTokens(USER_ID);
        assertThat(response.getRoles()).containsExactly("PLAYER");
    }

    @Test
    @DisplayName("adds requested role when user has no existing roles")
    void shouldAddRoleWhenUserHasNoExistingRoles() {
        UserAccountEntity user = user();
        RoleEntity player = role(4L, "PLAYER");

        when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("PLAYER")).thenReturn(Optional.of(player));
        when(userRoleRepository.findByUserId(USER_ID))
                .thenReturn(List.of())
                .thenReturn(List.of(userRole(user, player)));

        UserManagementResponse response = userManagementService.updateRoles(
                USER_ID,
                request("PLAYER")
        );

        verify(userRoleRepository, never()).deleteAll(any());
        ArgumentCaptor<Iterable<UserRoleEntity>> saveAllCaptor = userRoleIterableCaptor();
        verify(userRoleRepository).saveAll(saveAllCaptor.capture());
        List<UserRoleEntity> savedRelations = toList(saveAllCaptor.getValue());
        assertThat(savedRelations).hasSize(1);
        assertThat(savedRelations.get(0).getUser()).isSameAs(user);
        assertThat(savedRelations.get(0).getRole()).isSameAs(player);

        verify(refreshTokenService).revokeAllUserRefreshTokens(USER_ID);
        assertThat(response.getRoles()).containsExactly("PLAYER");
    }

    @Test
    @DisplayName("throws when requested role does not exist")
    void shouldThrowWhenRequestedRoleDoesNotExist() {
        UserAccountEntity user = user();

        when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("UNKNOWN_ROLE")).thenReturn(Optional.empty());

        Throwable throwable = catchThrowable(() -> userManagementService.updateRoles(
                USER_ID,
                request("UNKNOWN_ROLE")
        ));

        assertThat(throwable).isInstanceOf(BusinessException.class);
        BusinessException exception = (BusinessException) throwable;
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ROLE_NOT_FOUND);
        assertThat(exception).hasMessageContaining("UNKNOWN_ROLE");
        verifyNoInteractions(userRoleRepository, refreshTokenService);
    }

    @Test
    @DisplayName("throws when user does not exist")
    void shouldThrowWhenUserDoesNotExist() {
        when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementService.updateRoles(
                USER_ID,
                request("PLAYER")
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verifyNoInteractions(roleRepository, userRoleRepository, refreshTokenService);
    }

    @Test
    @DisplayName("normalizes duplicate role codes before replacing roles")
    void shouldProcessUniqueRolesWhenRequestContainsDuplicateRoleCodesAfterNormalization() {
        UserAccountEntity user = user();
        RoleEntity coach = role(2L, "COACH");
        RoleEntity player = role(4L, "PLAYER");
        UserRoleEntity existingCoachRelation = userRole(user, coach);

        when(userAccountRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("COACH")).thenReturn(Optional.of(coach));
        when(roleRepository.findByCode("PLAYER")).thenReturn(Optional.of(player));
        when(userRoleRepository.findByUserId(USER_ID))
                .thenReturn(List.of(existingCoachRelation))
                .thenReturn(List.of(existingCoachRelation, userRole(user, player)));

        UserManagementResponse response = userManagementService.updateRoles(
                USER_ID,
                request("COACH", " role_coach ", "PLAYER")
        );

        verify(roleRepository, times(1)).findByCode("COACH");
        verify(roleRepository, times(1)).findByCode("PLAYER");
        verify(userRoleRepository, never()).deleteAll(any());

        ArgumentCaptor<Iterable<UserRoleEntity>> saveAllCaptor = userRoleIterableCaptor();
        verify(userRoleRepository).saveAll(saveAllCaptor.capture());
        List<UserRoleEntity> savedRelations = toList(saveAllCaptor.getValue());
        assertThat(savedRelations).hasSize(1);
        assertThat(savedRelations.get(0).getRole()).isSameAs(player);

        verify(refreshTokenService).revokeAllUserRefreshTokens(USER_ID);
        assertThat(response.getRoles()).containsExactlyInAnyOrder("COACH", "PLAYER");
    }

    private static UpdateUserRolesRequest request(String... roleCodes) {
        return UpdateUserRolesRequest.builder()
                .roleCodes(new LinkedHashSet<>(List.of(roleCodes)))
                .build();
    }

    private static UserAccountEntity user() {
        return UserAccountEntity.builder()
                .id(USER_ID)
                .username("coach1")
                .passwordHash("hash")
                .displayName("Coach One")
                .status(UserStatus.ACTIVE)
                .build();
    }

    private static RoleEntity role(Long id, String code) {
        return RoleEntity.builder()
                .id(id)
                .code(code)
                .name(code)
                .build();
    }

    private static UserRoleEntity userRole(UserAccountEntity user, RoleEntity role) {
        return UserRoleEntity.builder()
                .user(user)
                .role(role)
                .build();
    }

    private static List<UserRoleEntity> toList(Iterable<UserRoleEntity> relations) {
        List<UserRoleEntity> result = new ArrayList<>();
        relations.forEach(result::add);
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<Iterable<UserRoleEntity>> userRoleIterableCaptor() {
        return ArgumentCaptor.forClass((Class) Iterable.class);
    }
}
