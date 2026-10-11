package com.apihealth.monitoring;

import com.apihealth.auth.dto.AuthResponse;
import com.apihealth.auth.dto.RegisterRequest;
import com.apihealth.common.response.ApiResponse;
import com.apihealth.monitoring.dto.CreateApiRequest;
import com.apihealth.monitoring.entity.HttpMethod;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiPlatformIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetToken(String email, String password) throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setPassword(password);
        req.setName("Test User");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<AuthResponse> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {});
        return response.getData().getToken();
    }

    @Test
    @DisplayName("Complete end-to-end flow: Create API, list, check dashboard, and verify cache")
    void testEndToEndFlow() throws Exception {
        String token = registerAndGetToken("flowuser@example.com", "SecretPass123!");

        // 1. Create Monitored API
        CreateApiRequest createReq = new CreateApiRequest();
        createReq.setName("Example Public Service");
        createReq.setUrl("https://api.github.com/zen");
        createReq.setHttpMethod(HttpMethod.GET);
        createReq.setCheckIntervalSeconds(60);
        createReq.setTimeoutMillis(5000);
        createReq.setLatencyThresholdMillis(300);

        MvcResult createResult = mockMvc.perform(post("/api/v1/apis")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Example Public Service"))
                .andReturn();

        // 2. Query APIs list
        mockMvc.perform(get("/api/v1/apis")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());

        // 3. Query Dashboard Summary
        mockMvc.perform(get("/api/v1/metrics/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalApis").isNumber());

        // 4. Query Cache Stats
        mockMvc.perform(get("/api/v1/cache/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inMemorySize").isNumber());

        // 5. Run Cache Benchmark
        mockMvc.perform(get("/api/v1/cache/benchmark?iterations=20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.inMemorySpeedupMultiplier").isNumber());

        // 6. Query Alerts
        mockMvc.perform(get("/api/v1/alerts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Should prevent cross-user resource access (Ownership Isolation)")
    void testOwnershipIsolation() throws Exception {
        String tokenUserA = registerAndGetToken("usera@example.com", "Password123!");
        String tokenUserB = registerAndGetToken("userb@example.com", "Password123!");

        // User A creates an API
        CreateApiRequest createReq = new CreateApiRequest();
        createReq.setName("User A API");
        createReq.setUrl("https://api.github.com/zen");
        createReq.setHttpMethod(HttpMethod.GET);

        MvcResult createResult = mockMvc.perform(post("/api/v1/apis")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String body = createResult.getResponse().getContentAsString();
        long apiId = objectMapper.readTree(body).get("data").get("id").asLong();

        // User B tries to read User A's API -> Should be 403 Forbidden
        mockMvc.perform(get("/api/v1/apis/" + apiId)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
