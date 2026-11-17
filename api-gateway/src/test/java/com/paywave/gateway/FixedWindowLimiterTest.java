package com.paywave.gateway;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FixedWindowLimiterTest {
    @Test
    void blocksAfterTheLimitInsideTheSameMinute() {
        FixedWindowLimiter limiter = new FixedWindowLimiter();
        Instant now = Instant.parse("2026-11-17T06:00:10Z");
        assertThat(limiter.allow("1.1.1.1:auth", 2, now)).isTrue();
        assertThat(limiter.allow("1.1.1.1:auth", 2, now)).isTrue();
        assertThat(limiter.allow("1.1.1.1:auth", 2, now)).isFalse();
        assertThat(limiter.allow("1.1.1.1:auth", 2, now.plusSeconds(60))).isTrue();
    }
}
