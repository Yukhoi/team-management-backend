Feature: Admin updates user roles

  Background:
    Given the following roles exist
      | ADMIN  |
      | COACH  |
      | PLAYER |

  Scenario: Existing role and new role are submitted without inserting a duplicate relation
    Given a test user has roles
      | COACH |
    When the administrator updates the user roles to
      | COACH  |
      | PLAYER |
    Then the HTTP status should be 200
    And the user roles in the database should be
      | COACH  |
      | PLAYER |
    And the user should not have duplicate user_role records

  Scenario: Submitting the same roles again succeeds and remains idempotent
    Given a test user has roles
      | COACH  |
      | PLAYER |
    When the administrator updates the user roles to
      | COACH  |
      | PLAYER |
    And the administrator updates the user roles again to
      | COACH  |
      | PLAYER |
    Then the HTTP status should be 200
    And the user roles in the database should be
      | COACH  |
      | PLAYER |
    And the user should not have duplicate user_role records

  Scenario: Removed roles are deleted and requested roles are retained
    Given a test user has roles
      | ADMIN |
      | COACH |
    When the administrator updates the user roles to
      | ADMIN |
    Then the HTTP status should be 200
    And the user roles in the database should be
      | ADMIN |
    And the user should not have duplicate user_role records

  Scenario: Old roles are deleted and a new role is added
    Given a test user has roles
      | ADMIN |
      | COACH |
    When the administrator updates the user roles to
      | PLAYER |
    Then the HTTP status should be 200
    And the user roles in the database should be
      | PLAYER |
    And the user should not have duplicate user_role records

  Scenario: A user with no roles can receive a role
    Given a test user has no roles
    When the administrator updates the user roles to
      | PLAYER |
    Then the HTTP status should be 200
    And the user roles in the database should be
      | PLAYER |
    And the user should not have duplicate user_role records

  Scenario: Updating to an unknown role returns an error and leaves the database unchanged
    Given a test user has roles
      | COACH |
    When the administrator updates the user roles to
      | UNKNOWN_ROLE |
    Then the HTTP status should be 400
    And the error code should be "ROLE_NOT_FOUND"
    And the user roles in the database should still be
      | COACH |
    And the user should not have duplicate user_role records

  Scenario: Updating roles for a missing user returns an error
    Given the target user does not exist
    When the administrator updates the user roles to
      | PLAYER |
    Then the HTTP status should be 404
    And the error code should be "USER_NOT_FOUND"

  Scenario: Updating user roles revokes the user's existing refresh token
    Given a test user has roles
      | COACH |
    And the user has a valid refresh token
    When the administrator updates the user roles to
      | PLAYER |
    Then the HTTP status should be 200
    And the user roles in the database should be
      | PLAYER |
    And the user's refresh token should be revoked
