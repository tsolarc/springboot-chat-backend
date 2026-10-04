package com.Backend.Security.Config;

import com.Backend.Common.Http.ApiProblems;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class MonitoringSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain monitoringSecurityFilterChain(HttpSecurity http, PasswordEncoder encoder,
            ApiProblems problems, @Value("${monitoring.username:prometheus}") String username,
            @Value("${monitoring.password:}") String password) throws Exception {
        var users = new InMemoryUserDetailsManager();
        // An unset password leaves monitoring closed, without introducing a default credential.
        if (!password.isBlank()) {
            users.createUser(User.withUsername(username).password(encoder.encode(password))
                    .roles("MONITORING").build());
        }
        var provider = new DaoAuthenticationProvider(encoder);
        provider.setUserDetailsService(users);
        AuthenticationEntryPoint unauthorized = (request, response, failure) -> {
            response.setHeader("WWW-Authenticate", "Basic realm=\"monitoring\"");
            problems.write(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                    "Monitoring credentials are required");
        };
        return http.securityMatcher("/actuator/prometheus", "/actuator/info")
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationManager(new ProviderManager(provider))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/prometheus", "/actuator/info")
                        .hasRole("MONITORING")
                        .anyRequest().denyAll())
                .httpBasic(basic -> basic.authenticationEntryPoint(unauthorized))
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler((request, response, failure) -> problems.write(request, response,
                                HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied")))
                .build();
    }
}
