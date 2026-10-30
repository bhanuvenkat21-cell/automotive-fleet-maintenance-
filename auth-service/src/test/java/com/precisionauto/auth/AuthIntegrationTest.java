package com.precisionauto.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.precisionauto.common.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"})
@AutoConfigureMockMvc
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired JwtService jwt;

    private String body(String u, String p, String extra) {
        return "{\"username\":\"" + u + "\",\"password\":\"" + p + "\"" + extra + "}";
    }

    @Test
    void registerThenLoginReturnsValidJwtWithClientRole() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body("alice", "Password1!", "")))
                .andExpect(status().isCreated());
        String json = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("alice", "Password1!", "")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode n = om.readTree(json);
        assertEquals("CLIENT", jwt.parse(n.get("token").asText()).get("role", String.class));
        assertEquals("alice", jwt.parse(n.get("token").asText()).getSubject());
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body("tech1", "nope-nope", "")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void technicianRegistrationNeedsInviteCode() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("bob", "Password1!", ",\"role\":\"TECHNICIAN\"")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(body("bob", "Password1!", ",\"role\":\"TECHNICIAN\",\"inviteCode\":\"GARAGE-STAFF-2026\"")))
                .andExpect(status().isCreated());
    }

    @Test
    void duplicateUsernameIs409() throws Exception {
        String b = body("carol", "Password1!", "");
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(b)).andExpect(status().isCreated());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(b)).andExpect(status().isConflict());
    }
}
