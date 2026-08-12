package com.midtone.backend.nap.application;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.nap.domain.NapSession;
import com.midtone.backend.nap.domain.NapSessionRepository;
import com.midtone.backend.nap.domain.NapStatus;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    public ActiveNap startNap(Integer requestedMinutes) {
        long userId = currentUserIdProvider.getCurrentUserId();
        if (napSessionRepository.findFirstByUserIdAndStatusOrderByStartedAtDesc(userId, NapStatus.RUNNING).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 진행 중인 낮잠 타이머가 있습니다.");
        }

        int plannedMinutes = requestedMinutes == null ? 20 : requestedMinutes;
        if (plannedMinutes < 1 || plannedMinutes > 180) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "낮잠 시간은 1분 이상 180분 이하여야 합니다.");
        }

        NapSession nap = napSessionRepository.save(new NapSession(userId, plannedMinutes, java.time.LocalDateTime.now(DEFAULT_ZONE)));
        return toActiveNap(nap);
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
