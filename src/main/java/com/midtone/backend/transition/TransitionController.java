package com.midtone.backend.transition;

import com.midtone.backend.transition.application.TransitionGuideResponse;
import com.midtone.backend.transition.application.TransitionListResponse;
import com.midtone.backend.transition.application.TransitionService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transitions")
public class TransitionController {

    private final TransitionService transitionService;

    public TransitionController(TransitionService transitionService) {
        this.transitionService = transitionService;
    }

    @GetMapping
    public TransitionListResponse getTransitions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return transitionService.getTransitions(from, to);
    }

    @GetMapping("/{date}")
    public TransitionGuideResponse getTransitionGuide(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return transitionService.getTransitionGuide(date);
    }
}
