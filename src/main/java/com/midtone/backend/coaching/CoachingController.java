package com.midtone.backend.coaching;

import com.midtone.backend.coaching.application.CoachingCardDetailResponse;
import com.midtone.backend.coaching.application.CoachingService;
import com.midtone.backend.coaching.application.RegenerateCoachingRequest;
import com.midtone.backend.coaching.application.RegenerateCoachingResponse;
import com.midtone.backend.coaching.application.TodayCoachingResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CoachingController {

    private final CoachingService coachingService;

    public CoachingController(CoachingService coachingService) {
        this.coachingService = coachingService;
    }

    @GetMapping("/coachings")
    public TodayCoachingResponse getTodayCoaching(@RequestParam(required = false) LocalDate date) {
        return coachingService.getTodayCoaching(date == null ? LocalDate.now() : date);
    }

    @GetMapping("/coachings/cards/{cardId}")
    public CoachingCardDetailResponse getCardDetail(@PathVariable Long cardId) {
        return coachingService.getCardDetail(cardId);
    }

    @PostMapping("/coachings:regenerate")
    public RegenerateCoachingResponse regenerateCoaching(@Valid @RequestBody RegenerateCoachingRequest request) {
        return coachingService.regenerateCoaching(request);
    }
}
