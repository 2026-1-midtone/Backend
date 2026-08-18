package com.midtone.backend.sleep.application;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.sleep.domain.SleepLog;
import com.midtone.backend.sleep.domain.SleepLogRepository;
import com.midtone.backend.sleep.domain.SleepLogSource;
import com.midtone.backend.user.domain.User;
import com.midtone.backend.user.domain.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SleepLogService {

    private final CurrentUserIdProvider currentUserIdProvider;
    private final SleepLogRepository sleepLogRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public SleepLogService(
            CurrentUserIdProvider currentUserIdProvider,
            SleepLogRepository sleepLogRepository,
            UserRepository userRepository,
            Clock clock) {
        this.currentUserIdProvider = currentUserIdProvider;
        this.sleepLogRepository = sleepLogRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public SleepLogResponse create(CreateSleepLogRequest request) {
        long userId = currentUserIdProvider.getCurrentUserId();
        ZoneId zone = userZone(userId);
        validateRequired(request.sleptAt(), request.wokeAt());
        validateInstants(request.sleptAt(), request.wokeAt());
        LocalDateTime sleptAt = toLocal(request.sleptAt(), zone);
        LocalDateTime wokeAt = toLocal(request.wokeAt(), zone);
        if (sleepLogRepository.countByUserIdAndSleptAtLessThanAndWokeAtGreaterThan(
                userId, wokeAt, sleptAt) > 0) {
            throw new SleepLogException(SleepLogException.ErrorCode.OVERLAPPING_LOG);
        }
        SleepLog saved = sleepLogRepository.save(new SleepLog(
                userId, sleptAt, wokeAt, zone.getId(), parseSource(request.source(), SleepLogSource.MANUAL)));
        return SleepLogResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public SleepLogListResponse getLogs(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new SleepLogException(SleepLogException.ErrorCode.INVALID_RANGE);
        }
        long userId = currentUserIdProvider.getCurrentUserId();
        userZone(userId);
        List<SleepLogResponse> logs = sleepLogRepository.findByUserIdAndWokeAtBetweenOrderByWokeAtAsc(
                        userId, from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
                .map(SleepLogResponse::from)
                .toList();
        return new SleepLogListResponse(logs);
    }

    @Transactional
    public SleepLogResponse update(long sleepLogId, UpdateSleepLogRequest request) {
        long userId = currentUserIdProvider.getCurrentUserId();
        SleepLog log = ownedLog(sleepLogId, userId);
        ZoneId zone = userZone(userId);
        OffsetDateTime sleptOffset = request.sleptAt() == null
                ? atRecordedZone(log.getSleptAt(), log.getRecordedTimezone()) : request.sleptAt();
        OffsetDateTime wokeOffset = request.wokeAt() == null
                ? atRecordedZone(log.getWokeAt(), log.getRecordedTimezone()) : request.wokeAt();
        validateInstants(sleptOffset, wokeOffset);
        LocalDateTime sleptAt = toLocal(sleptOffset, zone);
        LocalDateTime wokeAt = toLocal(wokeOffset, zone);
        if (sleepLogRepository.countByUserIdAndIdNotAndSleptAtLessThanAndWokeAtGreaterThan(
                userId, sleepLogId, wokeAt, sleptAt) > 0) {
            throw new SleepLogException(SleepLogException.ErrorCode.OVERLAPPING_LOG);
        }
        log.update(sleptAt, wokeAt, zone.getId(), parseSource(request.source(), log.getSource()));
        return SleepLogResponse.from(log);
    }

    @Transactional
    public void delete(long sleepLogId) {
        long userId = currentUserIdProvider.getCurrentUserId();
        sleepLogRepository.delete(ownedLog(sleepLogId, userId));
    }

    private SleepLog ownedLog(long sleepLogId, long userId) {
        SleepLog log = sleepLogRepository.findById(sleepLogId)
                .orElseThrow(() -> new SleepLogException(SleepLogException.ErrorCode.LOG_NOT_FOUND));
        if (log.getUserId() != userId) {
            throw new SleepLogException(SleepLogException.ErrorCode.ACCESS_DENIED);
        }
        return log;
    }

    private ZoneId userZone(long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SleepLogException(SleepLogException.ErrorCode.USER_NOT_FOUND));
        return ZoneId.of(user.getTimezone());
    }

    private void validateRequired(OffsetDateTime sleptAt, OffsetDateTime wokeAt) {
        if (sleptAt == null || wokeAt == null) {
            throw new SleepLogException(SleepLogException.ErrorCode.INVALID_INTERVAL);
        }
    }

    private void validateInstants(OffsetDateTime sleptAt, OffsetDateTime wokeAt) {
        if (!wokeAt.toInstant().isAfter(sleptAt.toInstant())) {
            throw new SleepLogException(SleepLogException.ErrorCode.INVALID_INTERVAL);
        }
        Instant now = clock.instant();
        if (sleptAt.toInstant().isAfter(now) || wokeAt.toInstant().isAfter(now)) {
            throw new SleepLogException(SleepLogException.ErrorCode.FUTURE_RECORD);
        }
    }

    private SleepLogSource parseSource(String source, SleepLogSource defaultSource) {
        if (source == null) {
            return defaultSource;
        }
        try {
            return SleepLogSource.valueOf(source);
        } catch (IllegalArgumentException exception) {
            throw new SleepLogException(SleepLogException.ErrorCode.INVALID_SOURCE);
        }
    }

    private LocalDateTime toLocal(OffsetDateTime value, ZoneId zone) {
        return value.atZoneSameInstant(zone).toLocalDateTime();
    }

    private OffsetDateTime atRecordedZone(LocalDateTime value, String timezone) {
        return value.atZone(ZoneId.of(timezone)).toOffsetDateTime();
    }
}
