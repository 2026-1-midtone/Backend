package com.midtone.backend.transition.application;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.application.schedule.TransitionDetector;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransitionService {

    private static final int LOOKBACK_DAYS = 14;

    private final CurrentUserIdProvider currentUserIdProvider;
    private final ShiftScheduleRepository shiftScheduleRepository;
    private final TransitionDetector transitionDetector;
    private final TransitionProtocolCatalog transitionProtocolCatalog;

    public TransitionService(
            CurrentUserIdProvider currentUserIdProvider,
            ShiftScheduleRepository shiftScheduleRepository,
            TransitionDetector transitionDetector,
            TransitionProtocolCatalog transitionProtocolCatalog) {
        this.currentUserIdProvider = currentUserIdProvider;
        this.shiftScheduleRepository = shiftScheduleRepository;
        this.transitionDetector = transitionDetector;
        this.transitionProtocolCatalog = transitionProtocolCatalog;
    }

    @Transactional(readOnly = true)
    public TransitionListResponse getTransitions(LocalDate from, LocalDate to) {
        long userId = currentUserIdProvider.getCurrentUserId();
        List<ShiftSchedule> shifts = shiftScheduleRepository.findByUserIdAndWorkDateBetweenOrderByWorkDateAsc(
                userId, from.minusDays(LOOKBACK_DAYS), to);
        return new TransitionListResponse(collectTransitions(shifts, from));
    }

    @Transactional(readOnly = true)
    public TransitionGuideResponse getTransitionGuide(LocalDate date) {
        long userId = currentUserIdProvider.getCurrentUserId();
        ShiftSchedule shift = shiftScheduleRepository.findByUserIdAndWorkDate(userId, date)
                .orElseThrow(() -> new TransitionException(TransitionException.ErrorCode.NOT_A_TRANSITION_DAY));
        TransitionDetector.TransitionInfo info = transitionDetector
                .detectTransition(userId, date, shift.getShiftType())
                .orElseThrow(() -> new TransitionException(TransitionException.ErrorCode.NOT_A_TRANSITION_DAY));
        TransitionProtocol protocol = transitionProtocolCatalog.resolve(info.fromShiftType(), info.toShiftType());
        return TransitionGuideResponse.of(date, info, protocol);
    }

    private List<TransitionListResponse.Item> collectTransitions(List<ShiftSchedule> shifts, LocalDate rangeFrom) {
        List<TransitionListResponse.Item> items = new ArrayList<>();
        ShiftType previousType = null;
        for (ShiftSchedule shift : shifts) {
            previousType = collectTransitionForDay(items, shift, previousType, rangeFrom);
        }
        return items;
    }

    private ShiftType collectTransitionForDay(
            List<TransitionListResponse.Item> items,
            ShiftSchedule shift,
            ShiftType previousType,
            LocalDate rangeFrom) {
        ShiftType type = shift.getShiftType();
        boolean isTransition = previousType != null && type != ShiftType.OFF && type != previousType;
        if (isTransition && !shift.getWorkDate().isBefore(rangeFrom)) {
            items.add(buildItem(shift.getWorkDate(), previousType, type));
        }
        return type == ShiftType.OFF ? previousType : type;
    }

    private TransitionListResponse.Item buildItem(LocalDate date, ShiftType fromType, ShiftType toType) {
        TransitionProtocol protocol = transitionProtocolCatalog.resolve(fromType, toType);
        return new TransitionListResponse.Item(date.toString(), fromType.name(), toType.name(), protocol.protocolName());
    }
}
