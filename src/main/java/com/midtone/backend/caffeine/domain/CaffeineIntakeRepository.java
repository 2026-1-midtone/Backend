package com.midtone.backend.caffeine.domain;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaffeineIntakeRepository extends JpaRepository<CaffeineIntake, Long> {

    List<CaffeineIntake> findByUserIdAndConsumedAtBetweenOrderByConsumedAtAsc(
            long userId, LocalDateTime from, LocalDateTime to);

    List<CaffeineIntake> findByUserIdAndConsumedAtGreaterThanEqualAndConsumedAtLessThanOrderByConsumedAtAsc(
            long userId, LocalDateTime fromInclusive, LocalDateTime toExclusive);
}
