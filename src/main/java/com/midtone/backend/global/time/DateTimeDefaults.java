package com.midtone.backend.global.time;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimeDefaults {

    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");
    public static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

    private DateTimeDefaults() {
    }
}
