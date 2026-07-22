package com.yukai.team.identityservice.bdd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yukai.team.identityservice.entity.RefreshTokenEntity;
import com.yukai.team.identityservice.entity.RoleEntity;
import com.yukai.team.identityservice.entity.UserAccountEntity;
import com.yukai.team.identityservice.entity.UserRoleEntity;
import com.yukai.team.identityservice.enums.UserStatus;
import com.yukai.team.identityservice.repository.RefreshTokenRepository;
import com.yukai.team.identityservice.repository.RoleRepository;
import com.yukai.team.identityservice.repository.UserAccountRepository;
import com.yukai.team.identityservice.repository.UserRoleRepository;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class UserRoleSteps {

    private static final String TEST_USERNAME_PREFIX = "bdd-role-";

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final CucumberTestContext context;

    public UserRoleSteps(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            RefreshTokenRepository refreshTokenRepository,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            CucumberTestContext context
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.context = context;
    }

    @Before
    public void beforeScenario() {
        cleanupTestUsers();
    }

    @After
    public void afterScenario() {
        cleanupTestUsers();
    }

    @ParameterType("\\d+")
    public int statusCode(String value) {
        return Integer.parseInt(value);
    }

    @Given("the following roles exist")
    public void theFollowingRolesExist(DataTable dataTable) {
        dataTable.asList().forEach(this::ensureRoleExists);
    }

    @Given("a test user has roles")
    public void aTestUserHasRoles(DataTable dataTable) {
        UserAccountEntity user = createTestUser();
        assignRoles(user, roleCodes(dataTable));
        context.setUserId(user.getId());
    }

    @Given("a test user has no roles")
    public void aTestUserHasNoRoles() {
        UserAccountEntity user = createTestUser();
        context.setUserId(user.getId());
    }

    @Given("the target user does not exist")
    public void theTargetUserDoesNotExist() {
        context.setUserId(999_999_999L);
    }

    @Given("the user has a valid refresh token")
    public void theUserHasAValidRefreshToken() {
        UserAccountEntity user = userAccountRepository.findById(context.getUserId()).orElseThrow();
        RefreshTokenEntity refreshToken = refreshTokenRepository.save(RefreshTokenEntity.builder()
                .user(user)
                .token("bdd-refresh-" + UUID.randomUUID())
                .expiredAt(OffsetDateTime.now().plusDays(1))
                .revokedFlag(false)
                .build());
        context.setRefreshToken(refreshToken.getToken());
    }

    @When("the administrator updates the user roles to")
    public void theAdministratorUpdatesTheUserRolesTo(DataTable dataTable) throws Exception {
        updateRoles(roleCodes(dataTable));
    }

    @When("the administrator updates the user roles again to")
    public void theAdministratorUpdatesTheUserRolesAgainTo(DataTable dataTable) throws Exception {
        updateRoles(roleCodes(dataTable));
    }

    @Then("the HTTP status should be {statusCode}")
    public void theHttpStatusShouldBe(int statusCode) {
        assertThat(context.getResponseStatus()).isEqualTo(statusCode);
    }

    @Then("the error code should be {string}")
    public void theErrorCodeShouldBe(String errorCode) throws Exception {
        JsonNode body = objectMapper.readTree(context.getResponse().getContentAsString());
        assertThat(body.path("errorCode").asText()).isEqualTo(errorCode);
    }

    @Then("the user roles in the database should be")
    public void theUserRolesInTheDatabaseShouldBe(DataTable dataTable) {
        assertThat(findRoleCodes(context.getUserId()))
                .containsExactlyInAnyOrderElementsOf(roleCodes(dataTable));
    }

    @Then("the user should not have duplicate user_role records")
    public void theUserShouldNotHaveDuplicateUserRoleRecords() {
        List<Long> roleIds = findRoleIds(context.getUserId());
        assertThat(roleIds).hasSameSizeAs(new LinkedHashSet<>(roleIds));
    }

    @Then("the user roles in the database should still be")
    public void theUserRolesInTheDatabaseShouldStillBe(DataTable dataTable) {
        theUserRolesInTheDatabaseShouldBe(dataTable);
    }

    @Then("the user's refresh token should be revoked")
    public void theUsersRefreshTokenShouldBeRevoked() {
        RefreshTokenEntity refreshToken = refreshTokenRepository.findByToken(context.getRefreshToken()).orElseThrow();
        assertThat(refreshToken.getRevokedFlag()).isTrue();
    }

    private void updateRoles(List<String> roleCodes) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("roleCodes", roleCodes));
        context.setResponse(mockMvc.perform(put("/api/users/{id}/roles", context.getUserId())
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andReturn()
                .getResponse());
    }

    private UserAccountEntity createTestUser() {
        String username = TEST_USERNAME_PREFIX + UUID.randomUUID().toString().substring(0, 12);
        context.setUsername(username);
        return userAccountRepository.save(UserAccountEntity.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode("password"))
                .displayName("Role Test User")
                .status(UserStatus.ACTIVE)
                .build());
    }

    private void assignRoles(UserAccountEntity user, List<String> roleCodes) {
        List<UserRoleEntity> userRoles = roleCodes.stream()
                .map(this::ensureRoleExists)
                .map(role -> UserRoleEntity.builder()
                        .user(user)
                        .role(role)
                        .build())
                .toList();
        userRoleRepository.saveAll(userRoles);
    }

    private RoleEntity ensureRoleExists(String roleCode) {
        String normalizedCode = roleCode.trim().toUpperCase();
        return roleRepository.findByCode(normalizedCode)
                .orElseGet(() -> roleRepository.save(RoleEntity.builder()
                        .code(normalizedCode)
                        .name(normalizedCode)
                        .description("BDD role " + normalizedCode)
                        .build()));
    }

    private List<String> findRoleCodes(Long userId) {
        return jdbcTemplate.queryForList("""
                select r.code
                from identity.user_role ur
                join identity.role r on r.id = ur.role_id
                where ur.user_id = ?
                order by r.code
                """, String.class, userId);
    }

    private List<Long> findRoleIds(Long userId) {
        return jdbcTemplate.queryForList("""
                select ur.role_id
                from identity.user_role ur
                where ur.user_id = ?
                """, Long.class, userId);
    }

    private void cleanupTestUsers() {
        jdbcTemplate.update("""
                delete from identity.refresh_token
                where user_id in (
                    select id from identity.user_account where username like ?
                )
                """, TEST_USERNAME_PREFIX + "%");
        jdbcTemplate.update("""
                delete from identity.user_role
                where user_id in (
                    select id from identity.user_account where username like ?
                )
                """, TEST_USERNAME_PREFIX + "%");
        jdbcTemplate.update("delete from identity.user_account where username like ?", TEST_USERNAME_PREFIX + "%");
    }

    private List<String> roleCodes(DataTable dataTable) {
        Set<String> roleCodes = new LinkedHashSet<>();
        dataTable.asList().forEach(value -> roleCodes.add(value.trim().toUpperCase()));
        return List.copyOf(roleCodes);
    }
}
