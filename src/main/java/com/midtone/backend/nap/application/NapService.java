package com.midtone.backend.nap.application;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.nap.domain.NapSession;
import com.midtone.backend.nap.domain.NapSessionRepository;
import com.midtone.backend.nap.domain.NapStatus;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.stereotype.Service;

@Service
public class NapService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

    private final CurrentUserIdProvider currentUserIdProvider;
    private final NapSessionRepository napSessionRepository;

    public NapService(CurrentUserIdProvider currentUserIdProvider, NapSessionRepository napSessionRepository) {
        this.currentUserIdProvider = currentUserIdProvider;
        this.napSessionRepository = napSessionRepository;
    }

    public ActiveNap getActiveNap() {
        return napSessionRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(
                        currentUserIdProvider.getCurrentUserId(), NapStatus.RUNNING)
                .map(this::toActiveNap)
                .orElse(null);
    }

    private ActiveNap toActiveNap(NapSession nap) {
        OffsetDateTime startedAt = nap.getStartedAt().atZone(DEFAULT_ZONE).toOffsetDateTime();
        OffsetDateTime expectedEndAt = nap.getExpectedEndAt().atZone(DEFAULT_ZONE).toOffsetDateTime();
        long remainingSeconds = Math.max(0, Duration.between(OffsetDateTime.now(DEFAULT_ZONE), expectedEndAt).toSeconds());
        return new ActiveNap(nap.getId(), nap.getPlannedMinutes(), startedAt, expectedEndAt, remainingSeconds, nap.getStatus().name());
    }

    public record ActiveNap(Long napId, int plannedMinutes, OffsetDateTime startedAt, OffsetDateTime expectedEndAt,
                            long remainingSeconds, String status) {
    }
}
