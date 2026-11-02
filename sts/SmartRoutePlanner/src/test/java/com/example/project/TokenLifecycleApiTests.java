package com.example.project;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.project.member.repository.TravelUserRepository;
import com.example.project.security.token.TokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TokenLifecycleApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TokenRepository tokens;
    @Autowired TravelUserRepository users;

    @AfterEach void cleanup() { tokens.deleteAll(); users.deleteAll(); }

    JsonNode register() throws Exception {
        var response=mvc.perform(post("/api/v1/auth/register").contentType("application/json")
                .content("{\"email\":\"token@example.invalid\",\"password\":\"fixture-password\",\"nickname\":\"fixture\"}"))
                .andExpect(status().isOk()).andReturn().getResponse();
        return json.readTree(response.getContentAsString());
    }

    @Test void accessTokenCannotBeUsedAsRefreshToken() throws Exception {
        var account=register();
        // Baseline JWTs have second-resolution timestamps and no jti; avoid
        // accidentally reproducing their duplicate-token DB error instead.
        Thread.sleep(1100);
        mvc.perform(post("/api/v1/auth/refresh-token").header("Authorization","Bearer "+account.get("access_token").asText()))
                .andExpect(status().isUnauthorized());
    }

    JsonNode refresh(String token) throws Exception {
        var response=mvc.perform(post("/api/v1/auth/refresh-token")
                .header("Authorization","Bearer "+token))
                .andExpect(status().isOk()).andReturn().getResponse();
        return json.readTree(response.getContentAsString());
    }

    @Test void refreshRotatesBothTokensAndRejectsReplay() throws Exception {
        var first=register();
        var second=refresh(first.get("refresh_token").asText());
        org.junit.jupiter.api.Assertions.assertNotEquals(first.get("access_token"),second.get("access_token"));
        org.junit.jupiter.api.Assertions.assertNotEquals(first.get("refresh_token"),second.get("refresh_token"));
        mvc.perform(post("/api/v1/auth/refresh-token").header("Authorization","Bearer "+first.get("refresh_token").asText()))
                .andExpect(status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/user/me")
                .header("Authorization","Bearer "+first.get("access_token").asText())).andExpect(status().isUnauthorized());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/user/me")
                .header("Authorization","Bearer "+second.get("access_token").asText())).andExpect(status().isOk());
    }

    @Test void refreshTokenIsNotAnApiCredential() throws Exception {
        var account=register();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/user/me")
                .header("Authorization","Bearer "+account.get("refresh_token").asText())).andExpect(status().isUnauthorized());
    }

    @Test void logoutRevokesRefreshTokenToo() throws Exception {
        var account=register();
        mvc.perform(post("/api/v1/auth/logout").header("Authorization","Bearer "+account.get("access_token").asText()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/refresh-token").header("Authorization","Bearer "+account.get("refresh_token").asText()))
                .andExpect(status().isUnauthorized());
    }

    @Test void deletionRevokesBothCredentials() throws Exception {
        var account=register();
        mvc.perform(post("/api/user/delete").header("Authorization","Bearer "+account.get("access_token").asText()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/refresh-token").header("Authorization","Bearer "+account.get("refresh_token").asText()))
                .andExpect(status().isUnauthorized());
    }

    @Test void missingAndMalformedRefreshCredentialsAreUnauthorized() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh-token")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh-token").header("Authorization","Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }
}
