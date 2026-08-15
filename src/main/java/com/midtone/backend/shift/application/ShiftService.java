package com.midtone.backend.shift.application;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftService {

    private final ShiftScheduleRepository shiftScheduleRepository;
    private final CurrentUserIdProvider currentUserIdProvider;

    public ShiftService(
            ShiftScheduleRepository shiftScheduleRepository, CurrentUserIdProvider currentUserIdProvider) {
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.currentUserIdProvider = currentUserIdProvider;
    }

    @Transactional
    public CreateShiftResponse createShift(CreateShiftRequest request) {
        long userId = currentUserIdProvider.getCurrentUserId();
        LocalDate workDate = LocalDate.parse(request.workDate());
        validateNoDuplicate(userId, workDate);
        ShiftSchedule shift = buildShift(userId, workDate, request);
        shiftScheduleRepository.save(shift);
        return CreateShiftResponse.from(shift);
    }

    private void validateNoDuplicate(long userId, LocalDate workDate) {
        if (shiftScheduleRepository.existsByUserIdAndWorkDate(userId, workDate)) {
            throw new DuplicateShiftException();
        }
    }

    private ShiftSchedule buildShift(long userId, LocalDate workDate, CreateShiftRequest request) {
        ShiftType shiftType = ShiftType.valueOf(request.shiftType());
        ShiftTime shiftTime = new ShiftTime(parseTime(request.startTime()), parseTime(request.endTime()));
        return new ShiftSchedule(userId, workDate, shiftType, shiftTime);
    }

    private LocalTime parseTime(String time) {
        return time == null ? null : LocalTime.parse(time);
    }
}
