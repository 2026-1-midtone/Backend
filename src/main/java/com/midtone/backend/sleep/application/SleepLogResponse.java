package com.midtone.backend.sleep.application;

import com.midtone.backend.sleep.domain.SleepLog;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public record SleepLogResponse(
        Long sleepLogId,
        OffsetDateTime sleptAt,
        OffsetDateTime wokeAt,
        String recordedTimezone,
        String source,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static SleepLogResponse from(SleepLog log) {
        ZoneId zone = ZoneId.of(log.getRecordedTimezone());
        return new SleepLogResponse(
                log.getId(),
                log.getSleptAt().atZone(zone).toOffsetDateTime(),
                log.getWokeAt().atZone(zone).toOffsetDateTime(),
                log.getRecordedTimezone(),
                log.getSource().name(),
                log.getCreatedAt().atZone(zone).toOffsetDateTime(),
                log.getUpdatedAt().atZone(zone).toOffsetDateTime());
    }
}
