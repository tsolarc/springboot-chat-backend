package com.Backend.Config;

import com.Backend.Chat.Limiter.ConcurrentSessionConnectionLimiter;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuración de pruebas para los limitadores de rate.
 * Esta configuración usa implementaciones permisivas que siempre permiten conexiones.
 */
@Configuration
@Profile("test")
public class TestRateLimitingConfig {

    @Bean
    public ConcurrentSessionConnectionLimiter sessionConnectionLimiter() {
        return new ConcurrentSessionConnectionLimiter(100);
    }

    @Bean
    public RateLimiter messageSendingRateLimiter() {
        return RateLimiter.create(1000.0);
    }
}
