package com.midtone.backend.nap.application;

import com.midtone.backend.global.time.DateTimeDefaults;
import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.nap.domain.NapSession;
import com.midtone.backend.nap.domain.NapSessionRepository;
import com.midtone.backend.nap.domain.NapStatus;
import com.midtone.backend.user.domain.UserSettingsRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NapService {

    private static final int MIN_NAP_MINUTES = 1;
    private static final int MAX_NAP_MINUTES = 180;

    private final CurrentUserIdProvider currentUserIdProvider;
    private final NapSessionRepository napSessionRepository;
    private final UserSettingsRepository userSettingsRepository;

    public NapService(CurrentUserIdProvider currentUserIdProvider, NapSessionRepository napSessionRepository,
                      UserSettingsRepository userSettingsRepository) {
        this.currentUserIdProvider = currentUserIdProvider;
        this.napSessionRepository = napSessionRepository;
        this.userSettingsRepository = userSettingsRepository;
    }

    public ActiveNap getActiveNap() {
        return napSessionRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(
                        currentUserIdProvider.getCurrentUserId(), NapStatus.RUNNING)
                .map(this::toActiveNap)
                .orElse(null);
    }

    public ActiveNap startNap(Integer requestedMinutes) {
        long userId = currentUserIdProvider.getCurrentUserId();
        if (napSessionRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(userId, NapStatus.RUNNING).isPresent()) {
            throw new NapException(NapException.ErrorCode.ALREADY_RUNNING);
        }

        com.midtone.backend.user.domain.UserSettings settings = userSettingsRepository.findById(userId)
                .orElseThrow(() -> new NapException(NapException.ErrorCode.USER_SETTINGS_NOT_FOUND));
        java.time.LocalDateTime now = java.time.LocalDateTime.now(DateTimeDefaults.DEFAULT_ZONE);
        long todayCount = napSessionRepository.countByUserIdAndStartedAtBetween(userId,
                now.toLocalDate().atStartOfDay(), now.toLocalDate().plusDays(1).atStartOfDay());
        if (todayCount >= settings.getMaxNapsPerDay()) {
            throw new NapException(NapException.ErrorCode.DAILY_LIMIT_EXCEEDED);
        }

        int plannedMinutes = requestedMinutes == null ? settings.getPreferredNapMinutes() : requestedMinutes;
        if (plannedMinutes < MIN_NAP_MINUTES || plannedMinutes > MAX_NAP_MINUTES) {
            throw new NapException(NapException.ErrorCode.INVALID_DURATION);
        }

        NapSession nap = napSessionRepository.save(new NapSession(userId, plannedMinutes, now));
        return toActiveNap(nap);
    }

    @Transactional
    public FinishedNap finishNap(long napId, String requestedStatus) {
        NapStatus status = parseFinishedStatus(requestedStatus);

        NapSession nap = napSessionRepository.findById(napId)
                .orElseThrow(() -> new NapException(NapException.ErrorCode.NAP_NOT_FOUND));
        if (nap.getUserId() != currentUserIdProvider.getCurrentUserId()) {
            throw new NapException(NapException.ErrorCode.ACCESS_DENIED);
        }
        if (nap.getStatus() != NapStatus.RUNNING) {
            throw new NapException(NapException.ErrorCode.ALREADY_FINISHED);
        }

        java.time.LocalDateTime endedAt = java.time.LocalDateTime.now(DateTimeDefaults.DEFAULT_ZONE);
        nap.finish(status, endedAt);
        int actualMinutes = (int) Math.max(0, Duration.between(nap.getStartedAt(), endedAt).toMinutes());
        return new FinishedNap(nap.getId(), status.name(), actualMinutes);
    }

    private NapStatus parseFinishedStatus(String requestedStatus) {
        NapStatus status;
        try {
            status = NapStatus.valueOf(requestedStatus);
        } catch (IllegalArgumentException exception) {
            throw new NapException(NapException.ErrorCode.INVALID_STATUS);
        }
        if (status == NapStatus.RUNNING) {
            throw new NapException(NapException.ErrorCode.INVALID_STATUS);
        }
        return status;
    }

    private ActiveNap toActiveNap(NapSession nap) {
        OffsetDateTime startedAt = nap.getStartedAt().atZone(DateTimeDefaults.DEFAULT_ZONE).toOffsetDateTime();
        OffsetDateTime expectedEndAt = nap.getExpectedEndAt().atZone(DateTimeDefaults.DEFAULT_ZONE).toOffsetDateTime();
        long remainingSeconds = Math.max(0, Duration.between(OffsetDateTime.now(DateTimeDefaults.DEFAULT_ZONE), expectedEndAt).toSeconds());
        return new ActiveNap(nap.getId(), nap.getPlannedMinutes(), startedAt, expectedEndAt, remainingSeconds, nap.getStatus().name());
    }

    public record ActiveNap(Long napId, int plannedMinutes, OffsetDateTime startedAt, OffsetDateTime expectedEndAt,
                            long remainingSeconds, String status) {
    }

    public record FinishedNap(Long napId, String status, int actualMinutes) {
    }
}
