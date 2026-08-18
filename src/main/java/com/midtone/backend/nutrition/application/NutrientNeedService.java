package com.midtone.backend.nutrition.application;

import com.midtone.backend.global.time.DateTimeDefaults;
import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.nutrition.domain.NutrientCode;
import com.midtone.backend.nutrition.domain.NutrientNeedSource;
import com.midtone.backend.nutrition.domain.UserNutrientNeed;
import com.midtone.backend.nutrition.domain.UserNutrientNeedRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NutrientNeedService {
    private final UserNutrientNeedRepository repository;
    private final CurrentUserIdProvider currentUserIdProvider;
    private final Clock clock;

    public NutrientNeedService(UserNutrientNeedRepository repository,
            CurrentUserIdProvider currentUserIdProvider, Clock clock) {
        this.repository = repository; this.currentUserIdProvider = currentUserIdProvider; this.clock = clock;
    }

    @Transactional
    public NutrientNeedResponse replace(SaveNutrientNeedsRequest request) {
        long userId = currentUserIdProvider.getCurrentUserId();
        LocalDate today = LocalDate.now(clock.withZone(DateTimeDefaults.DEFAULT_ZONE));
        Set<NutrientCode> duplicates = new HashSet<>();
        List<UserNutrientNeed> needs = request.needs().stream().map(item -> {
            NutrientCode code = parseNutrient(item.nutrientCode());
            if (!duplicates.add(code)) throw new NutrientException(NutrientException.ErrorCode.DUPLICATE_NUTRIENT);
            NutrientNeedSource source = parseSource(item.source());
            LocalDate recordedOn = parseDate(item.recordedOn(), today);
            return new UserNutrientNeed(userId, code, source, recordedOn);
        }).toList();
        repository.deleteAllByUserId(userId);
        repository.flush();
        repository.saveAll(needs);
        return response(needs);
    }

    @Transactional(readOnly = true)
    public NutrientNeedResponse get() {
        return get(currentUserIdProvider.getCurrentUserId());
    }

    @Transactional(readOnly = true)
    public NutrientNeedResponse get(long userId) {
        return response(repository.findAllByUserIdOrderByNutrientCodeAsc(userId));
    }

    @Transactional
    public void delete(String nutrientCode) {
        long userId = currentUserIdProvider.getCurrentUserId();
        UserNutrientNeed need = repository.findByUserIdAndNutrientCode(userId, parseNutrient(nutrientCode))
                .orElseThrow(() -> new NutrientException(NutrientException.ErrorCode.NOT_FOUND));
        repository.delete(need);
    }

    private NutrientNeedResponse response(List<UserNutrientNeed> needs) {
        return new NutrientNeedResponse(needs.stream().map(need -> new NutrientNeedResponse.Item(
                need.getNutrientCode().name(), need.getSource().name(), need.getRecordedOn())).toList());
    }

    private NutrientCode parseNutrient(String value) {
        try { return NutrientCode.valueOf(value); }
        catch (RuntimeException exception) { throw new NutrientException(NutrientException.ErrorCode.INVALID_NUTRIENT); }
    }

    private NutrientNeedSource parseSource(String value) {
        if (value == null || value.isBlank()) return NutrientNeedSource.USER_REPORTED;
        try { return NutrientNeedSource.valueOf(value); }
        catch (IllegalArgumentException exception) { throw new NutrientException(NutrientException.ErrorCode.INVALID_SOURCE); }
    }

    private LocalDate parseDate(String value, LocalDate today) {
        if (value == null || value.isBlank()) return today;
        try {
            LocalDate date = LocalDate.parse(value);
            if (date.isAfter(today)) throw new NutrientException(NutrientException.ErrorCode.INVALID_DATE);
            return date;
        } catch (DateTimeParseException exception) {
            throw new NutrientException(NutrientException.ErrorCode.INVALID_DATE);
        }
    }
}
