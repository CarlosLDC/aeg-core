package com.aeg.core.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimiter {

    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_TOKEN_ENDPOINT_PER_IP = 20;
    private static final int MAX_FORGOT_PASSWORD_PER_IP = 5;
    private static final int MAX_FORGOT_PASSWORD_PER_EMAIL = 3;
    private static final int MAX_BUCKETS = 2000;

    private final ConcurrentHashMap<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();

    public void checkTokenEndpoint(String ip) {
        String key = "token:" + normalizeIp(ip);
        if (!tryConsume(key, MAX_TOKEN_ENDPOINT_PER_IP)) {
            throw new TooManyRequestsException(
                    "Has realizado demasiados intentos. Espera unos minutos e inténtalo de nuevo."
            );
        }
    }

    public void checkForgotPasswordIp(String ip) {
        String key = "forgot-ip:" + normalizeIp(ip);
        if (!tryConsume(key, MAX_FORGOT_PASSWORD_PER_IP)) {
            throw new TooManyRequestsException(
                    "Has solicitado demasiados restablecimientos. Espera unos minutos e inténtalo de nuevo."
            );
        }
    }

    public boolean tryConsumeForgotPasswordEmail(String normalizedEmail) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return false;
        }
        return tryConsume("forgot-email:" + normalizedEmail, MAX_FORGOT_PASSWORD_PER_EMAIL);
    }

    public void reset() {
        buckets.clear();
    }

    private boolean tryConsume(String key, int maxRequests) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(WINDOW);
        cleanupIfNeeded(cutoff);

        Deque<Instant> timestamps = buckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    private void cleanupIfNeeded(Instant cutoff) {
        if (buckets.size() <= MAX_BUCKETS) {
            return;
        }
        Iterator<Map.Entry<String, Deque<Instant>>> iterator = buckets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Deque<Instant>> entry = iterator.next();
            Deque<Instant> queue = entry.getValue();
            synchronized (queue) {
                while (!queue.isEmpty() && queue.peekFirst().isBefore(cutoff)) {
                    queue.pollFirst();
                }
                if (queue.isEmpty()) {
                    iterator.remove();
                }
            }
        }
    }

    private static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        return ip.trim();
    }
}
