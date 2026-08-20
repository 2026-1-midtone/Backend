package com.midtone.backend.shift.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserShiftTimeDefaultRepository extends JpaRepository<UserShiftTimeDefault, Long> {

    List<UserShiftTimeDefault> findAllByUserId(long userId);

    void deleteAllByUserId(long userId);
}
