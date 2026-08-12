package com.midtone.backend.nap.domain;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NapSessionRepository extends JpaRepository<NapSession, Long> {

    Optional<NapSession> findFirstByUserIdAndStatusOrderByStartedAtDesc(long userId, NapStatus status);

    long countByUserIdAndStartedAtBetween(long userId, LocalDateTime start, LocalDateTime end);
}
