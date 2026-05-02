package com.yassine.portfolio_tracker.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiter {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        AttemptWindow window = attempts.get(key);
        if (window == null || window.isExpired()) {
            attempts.remove(key);
            return false;
        }
        return window.failures >= MAX_FAILURES;
    }

    public void recordFailure(String key) {
        attempts.compute(key, (ignored, current) -> {
            if (current == null || current.isExpired()) {
                return new AttemptWindow(1, Instant.now().plus(WINDOW));
            }
            current.failures++;
            return current;
        });
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private static class AttemptWindow {
        private int failures;
        private final Instant expiresAt;

        private AttemptWindow(int failures, Instant expiresAt) {
            this.failures = failures;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
