package com.midtone.backend.coaching.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachingCardRepository extends JpaRepository<CoachingCard, Long> {

    List<CoachingCard> findByDailyCoachingId(Long dailyCoachingId);

    void deleteByDailyCoachingId(Long dailyCoachingId);
}
