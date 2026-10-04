package com.Backend;

import com.Backend.Security.Service.JWTService;
import com.Backend.User.entity.User;
import com.Backend.User.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "monitoring.password=monitoring-test-password")
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test")
class MonitoringSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired JWTService jwt;
    @Autowired UserRepository users;

    @Test
    void monitoringAccountCanScrapePrometheus() throws Exception {
        mvc.perform(get("/actuator/prometheus").with(httpBasic("prometheus", "monitoring-test-password")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_memory_used_bytes")));
    }

    @Test
    void wrongMonitoringPasswordReturnsACorrelatedProblem() throws Exception {
        mvc.perform(get("/actuator/prometheus").header("X-Request-Id", "monitoring-123")
                        .with(httpBasic("prometheus", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"monitoring\""))
                .andExpect(jsonPath("$.requestId").value("monitoring-123"));
    }

    @Test
    void monitoringCredentialsCannotAccessPrivateApi() throws Exception {
        mvc.perform(get("/v1/conversations").with(httpBasic("prometheus", "monitoring-test-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void applicationJwtCannotAccessMonitoring() throws Exception {
        var user = users.save(User.builder().username("monitoring-isolation-test").build());
        String token = jwt.generateToken(user);
        mvc.perform(get("/v1/conversations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
