package com.midtone.backend.routine.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoutineTaskRepository extends JpaRepository<RoutineTask, Long> {

    List<RoutineTask> findAllByUserIdAndTaskDateOrderByIdAsc(long userId, LocalDate taskDate);

    List<RoutineTask> findAllByUserIdAndTaskDateBetweenOrderByTaskDateAscIdAsc(long userId, LocalDate from, LocalDate to);

    void deleteAllByUserIdAndTaskDateAndSourceTypeIn(long userId, LocalDate taskDate, Collection<String> sourceTypes);
}
