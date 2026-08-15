package com.midtone.backend.shift.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftScheduleRepository extends JpaRepository<ShiftSchedule, Long> {

    boolean existsByUserIdAndWorkDate(long userId, LocalDate workDate);

    List<ShiftSchedule> findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(long userId, LocalDate from, LocalDate to);
}
