package com.paywave.gateway;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public class FixedWindowLimiter {
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public boolean allow(String key, int limit, Instant now) {
        long bucket = now.getEpochSecond() / 60;
        Window window = windows.compute(key, (ignored, current) -> {
            if (current == null || current.bucket != bucket) {
                return new Window(bucket, 1);
            }
            current.count++;
            return current;
        });
        return window.count <= limit;
    }

    private static final class Window {
        private final long bucket;
        private int count;
        private Window(long bucket, int count) {
            this.bucket = bucket;
            this.count = count;
        }
    }
}
