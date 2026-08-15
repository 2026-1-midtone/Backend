package com.midtone.backend.shift.application.schedule;

import com.midtone.backend.coaching.application.CoachingService;
import com.midtone.backend.coaching.application.RegenerateCoachingRequest;
import com.midtone.backend.coaching.application.RegenerateCoachingResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ShiftCoachingRegenerationTrigger {

    private static final int SINGLE_UPDATE_LOOKAHEAD_DAYS = 1;

    private final CoachingService coachingService;

    public ShiftCoachingRegenerationTrigger(CoachingService coachingService) {
        this.coachingService = coachingService;
    }

    public List<String> triggerForSingleUpdate(LocalDate workDate) {
        return trigger(workDate, workDate.plusDays(SINGLE_UPDATE_LOOKAHEAD_DAYS));
    }

    public List<String> triggerForRange(LocalDate from, LocalDate to) {
        return trigger(from, to);
    }

    private List<String> trigger(LocalDate from, LocalDate to) {
        RegenerateCoachingResponse response =
                coachingService.regenerateCoaching(new RegenerateCoachingRequest(from.toString(), to.toString()));
        return response.regeneratedDates();
    }
}
