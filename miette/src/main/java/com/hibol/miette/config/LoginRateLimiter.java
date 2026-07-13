package com.hibol.miette.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());
        return bucket.getAvailableTokens() <= 0;
    }

    public void recordFailure(String ip) {
        buckets.computeIfAbsent(ip, k -> newBucket()).tryConsume(1);
    }

    public void reset(String ip) {
        buckets.remove(ip);
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(MAX_ATTEMPTS)
                .refillIntervally(MAX_ATTEMPTS, WINDOW)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
