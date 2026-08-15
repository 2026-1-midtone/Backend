package com.midtone.backend.shift.domain;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftScheduleRepository extends JpaRepository<ShiftSchedule, Long> {

    boolean existsByUserIdAndWorkDate(long userId, LocalDate workDate);
}
