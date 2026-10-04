package com.Backend.Chat.outbox;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component("stompRelay")
public class RealtimeBrokerHealth implements HealthIndicator, ApplicationListener<BrokerAvailabilityEvent> {
    private final AtomicBoolean available = new AtomicBoolean();

    @Override
    public void onApplicationEvent(BrokerAvailabilityEvent event) { available.set(event.isBrokerAvailable()); }

    public boolean isAvailable() { return available.get(); }

    @Override
    public Health health() { return available.get() ? Health.up().build() : Health.down().build(); }
}
