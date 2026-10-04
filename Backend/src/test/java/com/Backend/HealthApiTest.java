package com.Backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "monitoring.password=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthApiTest {
    @Autowired MockMvc mvc;
    @MockBean(name = "dbHealthContributor") HealthIndicator database;

    @BeforeEach
    void databaseAvailable() {
        when(database.health()).thenReturn(Health.up().withDetail("secret", "internal").build());
        when(database.getHealth(anyBoolean())).thenAnswer(invocation -> database.health());
    }

    @Test
    void readinessIsPublicAndHidesInternalDetails() throws Exception {
        mvc.perform(get("/health").header("X-Request-Id", "mobile-ticket-123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist())
                .andExpect(header().string("X-Request-Id", "mobile-ticket-123"));
    }

    @Test
    void databaseFailureStopsReadinessButNotLiveness() throws Exception {
        when(database.health()).thenReturn(Health.down().build());
        mvc.perform(get("/health")).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/livez")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void protectedApiReturnsCorrelated401Problem() throws Exception {
        mvc.perform(get("/v1/conversations").header("X-Request-Id", "ticket-456"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId").value("ticket-456"));
    }

    @Test
    void monitoringIsClosedWhenNoPasswordIsConfigured() throws Exception {
        mvc.perform(get("/actuator/info").with(
                org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                        .httpBasic("prometheus", "")))
                .andExpect(status().isUnauthorized());
    }
}
