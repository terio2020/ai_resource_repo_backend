package com.ai.repo.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TimezoneUtilTest {

    @Test
    void toAgentLocalTime_shouldConvertUtcToShanghai() {
        LocalDateTime utc = LocalDateTime.of(2026, 7, 12, 6, 0, 0);
        String result = TimezoneUtil.toAgentLocalTime(utc, "Asia/Shanghai");
        assertEquals("2026-07-12T14:00:00", result);
    }

    @Test
    void toAgentLocalTime_shouldConvertUtcToNewYork() {
        LocalDateTime utc = LocalDateTime.of(2026, 7, 12, 6, 0, 0);
        String result = TimezoneUtil.toAgentLocalTime(utc, "America/New_York");
        assertEquals("2026-07-12T02:00:00", result);
    }

    @Test
    void toAgentLocalTime_shouldConvertUtcToLondon() {
        LocalDateTime utc = LocalDateTime.of(2026, 7, 12, 6, 0, 0);
        String result = TimezoneUtil.toAgentLocalTime(utc, "Europe/London");
        assertEquals("2026-07-12T07:00:00", result);
    }

    @Test
    void toAgentLocalTime_shouldReturnNull_whenInputNull() {
        assertNull(TimezoneUtil.toAgentLocalTime(null, "Asia/Shanghai"));
    }

    @Test
    void toAgentLocalTime_shouldFallbackToShanghai_whenTimezoneNull() {
        LocalDateTime utc = LocalDateTime.of(2026, 7, 12, 6, 0, 0);
        String result = TimezoneUtil.toAgentLocalTime(utc, null);
        assertEquals("2026-07-12T14:00:00", result);
    }

    @Test
    void toAgentLocalTime_shouldFallbackToShanghai_whenTimezoneBlank() {
        LocalDateTime utc = LocalDateTime.of(2026, 7, 12, 6, 0, 0);
        String result = TimezoneUtil.toAgentLocalTime(utc, "");
        assertEquals("2026-07-12T14:00:00", result);
    }

    @Test
    void toAgentLocalTime_shouldFallbackToShanghai_whenTimezoneInvalid() {
        LocalDateTime utc = LocalDateTime.of(2026, 7, 12, 6, 0, 0);
        String result = TimezoneUtil.toAgentLocalTime(utc, "Invalid/Zone");
        assertEquals("2026-07-12T14:00:00", result);
    }

    @Test
    void toUtc_shouldConvertShanghaiToUtc() {
        LocalDateTime shanghai = LocalDateTime.of(2026, 7, 12, 14, 0, 0);
        LocalDateTime result = TimezoneUtil.toUtc(shanghai, "Asia/Shanghai");
        assertEquals(LocalDateTime.of(2026, 7, 12, 6, 0, 0), result);
    }

    @Test
    void toUtc_shouldConvertNewYorkToUtc() {
        LocalDateTime ny = LocalDateTime.of(2026, 7, 12, 2, 0, 0);
        LocalDateTime result = TimezoneUtil.toUtc(ny, "America/New_York");
        assertEquals(LocalDateTime.of(2026, 7, 12, 6, 0, 0), result);
    }

    @Test
    void toUtc_shouldReturnNull_whenInputNull() {
        assertNull(TimezoneUtil.toUtc(null, "Asia/Shanghai"));
    }

    @Test
    void toUtc_shouldFallbackToShanghai_whenTimezoneNull() {
        LocalDateTime shanghai = LocalDateTime.of(2026, 7, 12, 14, 0, 0);
        LocalDateTime result = TimezoneUtil.toUtc(shanghai, null);
        assertEquals(LocalDateTime.of(2026, 7, 12, 6, 0, 0), result);
    }

    @Test
    void toUtc_shouldFallbackToShanghai_whenTimezoneInvalid() {
        LocalDateTime shanghai = LocalDateTime.of(2026, 7, 12, 14, 0, 0);
        LocalDateTime result = TimezoneUtil.toUtc(shanghai, "Bad/Zone");
        assertEquals(LocalDateTime.of(2026, 7, 12, 6, 0, 0), result);
    }
}
