package com.midtone.backend.shift.application.pattern;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.application.ShiftException;
import com.midtone.backend.shift.domain.ShiftPattern;
import com.midtone.backend.shift.domain.ShiftPatternRepository;
import com.midtone.backend.shift.domain.ShiftType;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftPatternService {

    private final ShiftPatternRepository shiftPatternRepository;
    private final CurrentUserIdProvider currentUserIdProvider;

    public ShiftPatternService(
            ShiftPatternRepository shiftPatternRepository, CurrentUserIdProvider currentUserIdProvider) {
        this.shiftPatternRepository = shiftPatternRepository;
        this.currentUserIdProvider = currentUserIdProvider;
    }

    @Transactional(readOnly = true)
    public ShiftPatternListResponse getShiftPatterns() {
        long userId = currentUserIdProvider.getCurrentUserId();
        List<ShiftPattern> shiftPatterns = shiftPatternRepository.findByUserId(userId);
        return ShiftPatternListResponse.from(shiftPatterns);
    }

    @Transactional
    public ShiftPatternResponse saveShiftPattern(SaveShiftPatternRequest request) {
        long userId = currentUserIdProvider.getCurrentUserId();
        List<ShiftType> pattern = toShiftTypes(request.pattern());
        ShiftPattern shiftPattern = new ShiftPattern(userId, request.name(), pattern);
        shiftPatternRepository.save(shiftPattern);
        return ShiftPatternResponse.from(shiftPattern);
    }

    @Transactional
    public void deleteShiftPattern(Long patternId) {
        long userId = currentUserIdProvider.getCurrentUserId();
        ShiftPattern shiftPattern = shiftPatternRepository.findByIdAndUserId(patternId, userId)
                .orElseThrow(() -> new ShiftException(ShiftException.ErrorCode.SHIFT_PATTERN_NOT_FOUND));
        shiftPatternRepository.delete(shiftPattern);
    }

    private List<ShiftType> toShiftTypes(List<String> pattern) {
        return pattern.stream().map(ShiftType::valueOf).toList();
    }
}
