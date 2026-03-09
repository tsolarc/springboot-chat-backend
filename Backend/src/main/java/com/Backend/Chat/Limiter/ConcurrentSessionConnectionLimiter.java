package com.Backend.Chat.Limiter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ConcurrentSessionConnectionLimiter {

    private final Map<String, AtomicInteger> connectionCountPerUser = new ConcurrentHashMap<>();
    private final int maxSessionsPerUser;

    public ConcurrentSessionConnectionLimiter(@Value("${websocket.max-sessions-per-user:5}") int maxSessionsPerUser) {
        this.maxSessionsPerUser = maxSessionsPerUser;
    }

    public boolean allowConnection(String username) {
        AtomicInteger count = connectionCountPerUser.computeIfAbsent(username, k -> new AtomicInteger(0));
        int current = count.incrementAndGet();
        if (current > maxSessionsPerUser) {
            count.decrementAndGet();
            return false;
        }
        return true;
    }

    public void decrementConnectionCount(String username) {
        connectionCountPerUser.computeIfPresent(username, (k, v) -> {
            v.decrementAndGet();
            return v;
        });
    }
}
