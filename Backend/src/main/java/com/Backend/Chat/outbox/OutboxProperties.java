package com.Backend.Chat.outbox;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties("messaging.outbox")
public class OutboxProperties {
    @Min(1) @Max(500) private int batchSize = 50;
    @Min(1) @Max(32) private int workers = 4;
    @Min(1) @Max(100) private int maxAttempts = 12;
    private Duration confirmTimeout = Duration.ofSeconds(5);
    private Duration leaseDuration = Duration.ofSeconds(30);
    private Duration pollInterval = Duration.ofSeconds(1);

    @PostConstruct
    void validateDurations() {
        if (confirmTimeout.isNegative() || confirmTimeout.isZero() || pollInterval.isNegative()
                || pollInterval.isZero() || leaseDuration.compareTo(confirmTimeout.multipliedBy(2)) < 0) {
            throw new IllegalArgumentException("Outbox requires positive timeouts and a lease >= twice confirm-timeout");
        }
    }
}
