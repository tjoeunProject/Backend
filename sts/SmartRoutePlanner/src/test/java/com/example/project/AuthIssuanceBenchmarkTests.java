package com.example.project;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AuthIssuanceBenchmarkTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TokenRepository tokens;
    @Autowired TravelUserRepository users;
    @AfterEach void cleanup() { tokens.deleteAll(); users.deleteAll(); }
    @Test void benchmarkSuccessfulRegistration() throws Exception {
        int id=0;
        for(int batch=-1;batch<7;batch++) {
            long started=System.nanoTime();
            for(int n=0;n<3;n++) {
                var response=mvc.perform(post("/api/v1/auth/register").contentType("application/json")
                    .content("{\"email\":\"bench"+(id++)+"@example.invalid\",\"password\":\"fixture-password\",\"nickname\":\"fixture\"}"))
                    .andExpect(status().isOk()).andReturn().getResponse();
                var data=json.readTree(response.getContentAsString());
                assertThat(data.get("access_token").asText()).isNotBlank();
                assertThat(data.get("refresh_token").asText()).isNotBlank();
            }
            if(batch>=0) System.out.println("AUTH_REGISTER_MS="+(System.nanoTime()-started)/3_000_000.0);
        }
    }
}
