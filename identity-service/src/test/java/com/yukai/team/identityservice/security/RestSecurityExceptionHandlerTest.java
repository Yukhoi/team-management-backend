package com.yukai.team.identityservice.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RestSecurityExceptionHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldWriteUnauthorizedResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint(objectMapper).commence(
                new MockHttpServletRequest(),
                response,
                new BadCredentialsException("bad credentials")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(401, response.getStatus());
        assertFalse(body.get("success").asBoolean());
        assertEquals("UNAUTHORIZED", body.get("errorCode").asText());
        assertEquals("Authentication required", body.get("message").asText());
        assertEquals(0, body.get("details").size());
    }

    @Test
    void shouldWriteAccessDeniedResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAccessDeniedHandler(objectMapper).handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("denied")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertEquals(403, response.getStatus());
        assertFalse(body.get("success").asBoolean());
        assertEquals("ACCESS_DENIED", body.get("errorCode").asText());
        assertEquals("Access denied", body.get("message").asText());
        assertEquals(0, body.get("details").size());
    }
}
