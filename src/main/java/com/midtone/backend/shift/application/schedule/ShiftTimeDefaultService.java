package com.midtone.backend.shift.application.schedule;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.shift.application.ShiftException;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftTimeDefaults;
import com.midtone.backend.shift.domain.ShiftType;
import com.midtone.backend.shift.domain.UserShiftTimeDefault;
import com.midtone.backend.shift.domain.UserShiftTimeDefaultRepository;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 근무 유형별 기본 시작·종료 시각을 관리한다.
 * 시각이 비어 있는 근무는 코칭 카드·다음 근무·야간 영양 타이밍이 계산되지 않으므로,
 * 근무를 만들 때 시각을 생략하면 여기서 정한 값으로 채운다.
 */
@Service
public class ShiftTimeDefaultService {

    private static final String SOURCE_USER = "USER";
    private static final String SOURCE_SYSTEM = "SYSTEM";

    private final UserShiftTimeDefaultRepository repository;
    private final CurrentUserIdProvider currentUserIdProvider;

    public ShiftTimeDefaultService(
            UserShiftTimeDefaultRepository repository, CurrentUserIdProvider currentUserIdProvider) {
        this.repository = repository;
        this.currentUserIdProvider = currentUserIdProvider;
    }

    @Transactional(readOnly = true)
    public ShiftTimeDefaultsResponse get() {
        return toResponse(findByShiftType(currentUserIdProvider.getCurrentUserId()));
    }

    @Transactional
    public ShiftTimeDefaultsResponse replace(SaveShiftTimeDefaultsRequest request) {
        long userId = currentUserIdProvider.getCurrentUserId();
        List<UserShiftTimeDefault> saved = request.defaults().stream()
                .map(item -> toEntity(userId, item))
                .toList();
        rejectDuplicateTypes(saved);
        repository.deleteAllByUserId(userId);
        repository.flush();
        repository.saveAll(saved);
        return toResponse(byShiftType(saved));
    }

    /**
     * 근무를 만들 때 쓸 기본 시각. 사용자가 정한 값이 있으면 그걸, 없으면 대표값을 준다.
     * OFF 처럼 시각이 없는 유형은 빈 시각을 그대로 돌려준다.
     */
    @Transactional(readOnly = true)
    public ShiftTime resolve(long userId, ShiftType shiftType) {
        if (!ShiftTimeDefaults.isConfigurable(shiftType)) {
            return new ShiftTime(null, null);
        }
        UserShiftTimeDefault configured = findByShiftType(userId).get(shiftType);
        return configured == null ? ShiftTimeDefaults.of(shiftType) : configured.toShiftTime();
    }

    private UserShiftTimeDefault toEntity(long userId, SaveShiftTimeDefaultsRequest.Item item) {
        ShiftType shiftType = ShiftType.valueOf(item.shiftType());
        if (!ShiftTimeDefaults.isConfigurable(shiftType)) {
            throw new ShiftException(ShiftException.ErrorCode.SHIFT_TIME_DEFAULT_NOT_CONFIGURABLE);
        }
        return new UserShiftTimeDefault(
                userId, shiftType, LocalTime.parse(item.startTime()), LocalTime.parse(item.endTime()));
    }

    private void rejectDuplicateTypes(List<UserShiftTimeDefault> defaults) {
        Set<ShiftType> seen = new HashSet<>();
        for (UserShiftTimeDefault entry : defaults) {
            if (!seen.add(entry.getShiftType())) {
                throw new ShiftException(ShiftException.ErrorCode.DUPLICATE_SHIFT_TIME_DEFAULT);
            }
        }
    }

    private Map<ShiftType, UserShiftTimeDefault> findByShiftType(long userId) {
        return byShiftType(repository.findAllByUserId(userId));
    }

    private Map<ShiftType, UserShiftTimeDefault> byShiftType(List<UserShiftTimeDefault> defaults) {
        Map<ShiftType, UserShiftTimeDefault> byType = new EnumMap<>(ShiftType.class);
        defaults.forEach(entry -> byType.put(entry.getShiftType(), entry));
        return byType;
    }

    private ShiftTimeDefaultsResponse toResponse(Map<ShiftType, UserShiftTimeDefault> configured) {
        return new ShiftTimeDefaultsResponse(ShiftTimeDefaults.CONFIGURABLE_TYPES.stream()
                .map(shiftType -> toItem(shiftType, configured.get(shiftType)))
                .toList());
    }

    private ShiftTimeDefaultsResponse.Item toItem(ShiftType shiftType, UserShiftTimeDefault configured) {
        ShiftTime time = configured == null ? ShiftTimeDefaults.of(shiftType) : configured.toShiftTime();
        return new ShiftTimeDefaultsResponse.Item(
                shiftType.name(), time.startTime().toString(), time.endTime().toString(),
                configured == null ? SOURCE_SYSTEM : SOURCE_USER);
    }
}
