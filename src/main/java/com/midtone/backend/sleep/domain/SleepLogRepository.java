package com.midtone.backend.sleep.domain;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SleepLogRepository extends JpaRepository<SleepLog, Long> {

    List<SleepLog> findByUserIdAndWokeAtBetweenOrderByWokeAtAsc(
            long userId, LocalDateTime from, LocalDateTime to);

    long countByUserIdAndSleptAtLessThanAndWokeAtGreaterThan(
            long userId, LocalDateTime wokeAt, LocalDateTime sleptAt);

    long countByUserIdAndIdNotAndSleptAtLessThanAndWokeAtGreaterThan(
            long userId, long id, LocalDateTime wokeAt, LocalDateTime sleptAt);
}
