package com.aeg.core.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimiter {

    static final int MAX_BUCKETS = 2000;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int MAX_TOKEN_ENDPOINT_PER_IP = 20;
    private static final int MAX_FORGOT_PASSWORD_PER_IP = 5;
    private static final int MAX_FORGOT_PASSWORD_PER_EMAIL = 3;
    private static final int MAX_LOGIN_PER_IP = 40;
    private static final int MAX_LOGIN_FAILED_PER_ACCOUNT = 10;

    private final ConcurrentHashMap<String, BucketEntry> buckets = new ConcurrentHashMap<>();
    private final Object cleanupLock = new Object();

    public void checkLoginIp(String ip) {
        String key = "login-ip:" + normalizeIp(ip);
        if (!tryConsume(key, MAX_LOGIN_PER_IP)) {
            throw new TooManyRequestsException(
                    "Has realizado demasiados intentos de inicio de sesión. Espera unos minutos e inténtalo de nuevo."
            );
        }
    }

    public void checkLoginAccountLock(String ip, String username) {
        String key = loginFailureKey(ip, username);
        if (key == null) {
            return;
        }
        Instant cutoff = Instant.now().minus(WINDOW);
        BucketEntry entry = buckets.get(key);
        if (entry == null) {
            return;
        }
        synchronized (entry) {
            pruneExpired(entry.timestamps, cutoff);
            if (entry.timestamps.size() >= MAX_LOGIN_FAILED_PER_ACCOUNT) {
                throw new TooManyRequestsException(
                        "Demasiados intentos fallidos para esta cuenta. Espera unos minutos e inténtalo de nuevo."
                );
            }
        }
    }

    public void recordLoginFailure(String ip, String username) {
        String key = loginFailureKey(ip, username);
        if (key == null) {
            return;
        }
        tryConsume(key, Integer.MAX_VALUE);
    }

    public void clearLoginFailures(String ip, String username) {
        String key = loginFailureKey(ip, username);
        if (key != null) {
            buckets.remove(key);
        }
    }

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

    int bucketCount() {
        return buckets.size();
    }

    private boolean tryConsume(String key, int maxRequests) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(WINDOW);
        if (!buckets.containsKey(key)) {
            cleanupIfNeeded(cutoff);
        }

        BucketEntry entry = buckets.computeIfAbsent(key, ignored -> new BucketEntry(now));
        synchronized (entry) {
            pruneExpired(entry.timestamps, cutoff);
            entry.lastSeen = now;
            if (entry.timestamps.size() >= maxRequests) {
                return false;
            }
            entry.timestamps.addLast(now);
            return true;
        }
    }

    private void cleanupIfNeeded(Instant cutoff) {
        if (buckets.size() < MAX_BUCKETS) {
            return;
        }
        synchronized (cleanupLock) {
            if (buckets.size() < MAX_BUCKETS) {
                return;
            }
            Iterator<Map.Entry<String, BucketEntry>> iterator = buckets.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, BucketEntry> mapEntry = iterator.next();
                BucketEntry entry = mapEntry.getValue();
                synchronized (entry) {
                    pruneExpired(entry.timestamps, cutoff);
                    if (entry.timestamps.isEmpty()) {
                        iterator.remove();
                    }
                }
            }

            if (buckets.size() >= MAX_BUCKETS) {
                int targetSize = MAX_BUCKETS - Math.max(1, MAX_BUCKETS / 10);
                int toEvict = Math.max(1, buckets.size() - targetSize);
                List<Map.Entry<String, BucketEntry>> oldest = buckets.entrySet().stream()
                        .sorted(Comparator.comparing(e -> e.getValue().lastSeen))
                        .limit(toEvict)
                        .toList();
                for (Map.Entry<String, BucketEntry> candidate : oldest) {
                    buckets.remove(candidate.getKey(), candidate.getValue());
                }
            }
        }
    }

    private static void pruneExpired(Deque<Instant> timestamps, Instant cutoff) {
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
            timestamps.pollFirst();
        }
    }

    private static String loginFailureKey(String ip, String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return "login-fail:" + normalizeIp(ip) + ":" + username.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        return ip.trim();
    }

    private static final class BucketEntry {
        final Deque<Instant> timestamps = new ArrayDeque<>();
        volatile Instant lastSeen;

        BucketEntry(Instant initialSeen) {
            this.lastSeen = initialSeen;
        }
    }
}
