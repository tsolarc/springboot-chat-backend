package com.Backend.Chat.Limiter;

import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter que mantiene un límite independiente por usuario.
 * Evita que un usuario pueda bloquear a otros al consumir todo el rate limit global.
 */
public class PerUserRateLimiter {

    private final ConcurrentHashMap<String, RateLimiter> userRateLimiters = new ConcurrentHashMap<>();
    private final double permitsPerSecond;

    public PerUserRateLimiter(double permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
    }

    public boolean tryAcquire(String username) {
        if (username == null) {
            return false;
        }
        RateLimiter limiter = userRateLimiters.computeIfAbsent(username,
                k -> RateLimiter.create(permitsPerSecond));
        return limiter.tryAcquire();
    }

    public void removeUser(String username) {
        if (username != null) {
            userRateLimiters.remove(username);
        }
    }
}
