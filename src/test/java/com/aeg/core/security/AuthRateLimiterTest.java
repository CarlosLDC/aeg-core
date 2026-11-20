package com.aeg.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AuthRateLimiterTest {

    @Test
    void locksAccountAfterRepeatedLoginFailuresAndClearsOnSuccess() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        String ip = "198.51.100.10";
        String user = "User@Example.com";

        for (int i = 0; i < 10; i++) {
            limiter.checkLoginAccountLock(ip, user);
            limiter.recordLoginFailure(ip, user);
        }

        assertThatThrownBy(() -> limiter.checkLoginAccountLock(ip, "user@example.com"))
                .isInstanceOf(TooManyRequestsException.class);

        limiter.clearLoginFailures(ip, user);
        limiter.checkLoginAccountLock(ip, user);
    }

    @Test
    void keepsBucketCountBoundedUnderSpoofedIpFlood() {
        AuthRateLimiter limiter = new AuthRateLimiter();
        for (int i = 0; i < AuthRateLimiter.MAX_BUCKETS + 500; i++) {
            limiter.checkLoginIp("10.0." + (i / 256) + "." + (i % 256));
        }
        assertThat(limiter.bucketCount()).isLessThanOrEqualTo(AuthRateLimiter.MAX_BUCKETS);
    }
}
