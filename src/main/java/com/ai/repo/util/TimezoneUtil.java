package com.ai.repo.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class TimezoneUtil {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public static String toAgentLocalTime(LocalDateTime utcTime, String agentTimezone) {
        if (utcTime == null) return null;
        ZoneId zone = resolveZone(agentTimezone);
        ZonedDateTime utcZoned = utcTime.atZone(ZoneId.of("UTC"));
        ZonedDateTime localZoned = utcZoned.withZoneSameInstant(zone);
        return localZoned.format(ISO_FORMATTER);
    }

    public static LocalDateTime toUtc(LocalDateTime localTime, String agentTimezone) {
        if (localTime == null) return null;
        ZoneId zone = resolveZone(agentTimezone);
        ZonedDateTime localZoned = localTime.atZone(zone);
        return localZoned.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
    }

    private static ZoneId resolveZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of("Asia/Shanghai");
        }
        try {
            return ZoneId.of(timezone);
        } catch (Exception e) {
            return ZoneId.of("Asia/Shanghai");
        }
    }
}
