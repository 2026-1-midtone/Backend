package com.midtone.backend.shift.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftPatternRepository extends JpaRepository<ShiftPattern, Long> {

    List<ShiftPattern> findByUserId(long userId);

    Optional<ShiftPattern> findByIdAndUserId(Long id, long userId);
}
