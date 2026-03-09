package com.Backend.Chat.config.WebSocket;

import com.Backend.Chat.Limiter.ConcurrentSessionConnectionLimiter;
import com.Backend.Chat.Limiter.PerUserRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebSocketRateLimitingConfig {

    @Bean
    public ConcurrentSessionConnectionLimiter sessionConnectionLimiter() {
        return new ConcurrentSessionConnectionLimiter(5); // Máximo 5 conexiones por usuario
    }

    @Bean
    public PerUserRateLimiter perUserRateLimiter() {
        return new PerUserRateLimiter(10.0); // 10 mensajes por segundo por usuario
    }
}
