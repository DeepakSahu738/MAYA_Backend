package com.MAYA.MAYA.Service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory rate limiter.
 * Tracks requests per key (IP or sessionId) within a time window.
 * Resets automatically when the window expires.
 *
 * Limits:
 * - AI Chat: 20 requests per minute per session
 * - Strategy Generator: 5 requests per hour per session
 */
@Service
public class RateLimiterService {

    private final Map<String, RateBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Check if a request is allowed under the rate limit.
     *
     * @param key        - unique identifier (sessionId, IP, or userId)
     * @param maxRequests - max requests allowed in the window
     * @param windowMs   - time window in milliseconds
     * @return true if allowed, false if rate limited
     */
    public boolean isAllowed(String key, int maxRequests, long windowMs) {
        RateBucket bucket = buckets.compute(key, (k, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now - existing.windowStart > windowMs) {
                // Window expired — reset
                return new RateBucket(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        return bucket.count.get() <= maxRequests;
    }

    /**
     * Get remaining requests for a key.
     */
    public int getRemaining(String key, int maxRequests, long windowMs) {
        RateBucket bucket = buckets.get(key);
        if (bucket == null) return maxRequests;

        long now = System.currentTimeMillis();
        if (now - bucket.windowStart > windowMs) return maxRequests;

        return Math.max(0, maxRequests - bucket.count.get());
    }

    private static class RateBucket {
        final long windowStart;
        final AtomicInteger count;

        RateBucket(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
