package com.midtone.backend.routine.domain;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineTaskRepository extends JpaRepository<RoutineTask, Long> {

    List<RoutineTask> findAllByUserIdAndTaskDateOrderByIdAsc(long userId, LocalDate taskDate);
}
