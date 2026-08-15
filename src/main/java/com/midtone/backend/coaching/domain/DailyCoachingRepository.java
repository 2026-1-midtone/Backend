package com.midtone.backend.coaching.domain;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyCoachingRepository extends JpaRepository<DailyCoaching, Long> {

    Optional<DailyCoaching> findByUserIdAndCoachingDate(long userId, LocalDate coachingDate);
}
