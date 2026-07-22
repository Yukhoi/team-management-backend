package com.yukai.team.identityservice.bdd;

import io.cucumber.spring.ScenarioScope;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.stereotype.Component;

@Component
@ScenarioScope
public class CucumberTestContext {

    private Long userId;
    private String username;
    private String refreshToken;
    private MockHttpServletResponse response;

    Long getUserId() {
        return userId;
    }

    void setUserId(Long userId) {
        this.userId = userId;
    }

    String getUsername() {
        return username;
    }

    void setUsername(String username) {
        this.username = username;
    }

    String getRefreshToken() {
        return refreshToken;
    }

    void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    MockHttpServletResponse getResponse() {
        return response;
    }

    void setResponse(MockHttpServletResponse response) {
        this.response = response;
    }

    int getResponseStatus() {
        return response == null ? HttpStatus.INTERNAL_SERVER_ERROR.value() : response.getStatus();
    }
}
