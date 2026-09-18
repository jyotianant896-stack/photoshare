package com.trizen.photoshare;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends AbstractApiTest {

    @Test
    @DisplayName("a new lead can register and is given an admin token")
    void registerReturnsToken() throws Exception {
        String email = uniqueEmail("new-lead");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "New Lead", "email", email, "password", "Password@123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    @DisplayName("the same email cannot be registered twice")
    void duplicateEmailIsRejected() throws Exception {
        String email = uniqueEmail("dupe");
        registerAdmin("First", email, "Password@123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Second", "email", email, "password", "Password@123"))))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("a short password is rejected with field level detail")
    void weakPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Weak", "email", uniqueEmail("weak"), "password", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("a wrong password does not authenticate")
    void wrongPasswordIsUnauthorized() throws Exception {
        String email = uniqueEmail("login");
        registerAdmin("Lead", email, "Password@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "NotThePassword"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("the staff API is closed without a token")
    void eventsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a garbage token is not accepted")
    void tamperedTokenIsRejected() throws Exception {
        mockMvc.perform(get("/api/events").header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }
}
