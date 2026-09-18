package com.trizen.photoshare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Shared plumbing: a MockMvc client plus helpers to create accounts and events. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class AbstractApiTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String adminToken;
    protected Long adminId;

    @BeforeEach
    void createAdmin() throws Exception {
        adminToken = registerAdmin("Studio Lead", uniqueEmail("lead"), "Password@123");
        adminId = idOfCurrentUser(adminToken);
    }

    protected String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    protected String registerAdmin(String name, String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                new java.util.LinkedHashMap<>(java.util.Map.of(
                        "name", name, "email", email, "password", password)));
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        return json(result).get("token").asText();
    }

    protected String login(String email, String password) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "email", email, "password", password));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        return json(result).get("token").asText();
    }

    protected Long idOfCurrentUser(String token) throws Exception {
        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/api/auth/me").header("Authorization", bearer(token)))
                .andReturn();
        return json(result).get("id").asLong();
    }

    protected Long createEvent(String token, String name) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("name", name));
        MvcResult result = mockMvc.perform(post("/api/events")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        return json(result).get("id").asLong();
    }

    protected void addMember(String token, Long eventId, String name, String email, String password)
            throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "name", name, "email", email, "password", password));
        mockMvc.perform(post("/api/events/" + eventId + "/members")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
