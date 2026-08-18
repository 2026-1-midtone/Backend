package com.midtone.backend.caffeine;

import com.midtone.backend.caffeine.application.CaffeineIntakeListResponse;
import com.midtone.backend.caffeine.application.CaffeineIntakeResponse;
import com.midtone.backend.caffeine.application.CaffeineIntakeService;
import com.midtone.backend.caffeine.application.CreateCaffeineIntakeRequest;
import com.midtone.backend.caffeine.application.UpdateCaffeineIntakeRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/caffeine-intakes")
public class CaffeineIntakeController {

    private final CaffeineIntakeService caffeineIntakeService;

    public CaffeineIntakeController(CaffeineIntakeService caffeineIntakeService) {
        this.caffeineIntakeService = caffeineIntakeService;
    }

    @PostMapping
    public ResponseEntity<CaffeineIntakeResponse> create(
            @Valid @RequestBody CreateCaffeineIntakeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(caffeineIntakeService.create(request));
    }

    @GetMapping
    public ResponseEntity<CaffeineIntakeListResponse> getIntakes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(caffeineIntakeService.getIntakes(from, to));
    }

    @PatchMapping("/{intakeId}")
    public ResponseEntity<CaffeineIntakeResponse> update(
            @PathVariable long intakeId,
            @Valid @RequestBody UpdateCaffeineIntakeRequest request) {
        return ResponseEntity.ok(caffeineIntakeService.update(intakeId, request));
    }

    @DeleteMapping("/{intakeId}")
    public ResponseEntity<Void> delete(@PathVariable long intakeId) {
        caffeineIntakeService.delete(intakeId);
        return ResponseEntity.noContent().build();
    }
}
