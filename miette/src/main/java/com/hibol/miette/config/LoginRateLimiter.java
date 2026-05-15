package com.hibol.miette.config;

import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MS = 15 * 60 * 1000L;

    private final ConcurrentHashMap<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        Deque<Long> timestamps = attempts.get(ip);
        if (timestamps == null) return false;
        long cutoff = System.currentTimeMillis() - WINDOW_MS;
        timestamps.removeIf(t -> t < cutoff);
        return timestamps.size() >= MAX_ATTEMPTS;
    }

    public void recordFailure(String ip) {
        attempts.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>())
                .addLast(System.currentTimeMillis());
    }

    public void reset(String ip) {
        attempts.remove(ip);
    }
}
