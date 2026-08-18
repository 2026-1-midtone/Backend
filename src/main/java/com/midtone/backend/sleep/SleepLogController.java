package com.midtone.backend.sleep;

import com.midtone.backend.sleep.application.CreateSleepLogRequest;
import com.midtone.backend.sleep.application.SleepLogListResponse;
import com.midtone.backend.sleep.application.SleepLogResponse;
import com.midtone.backend.sleep.application.SleepLogService;
import com.midtone.backend.sleep.application.UpdateSleepLogRequest;
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
@RequestMapping("/api/v1/sleep-logs")
public class SleepLogController {

    private final SleepLogService sleepLogService;

    public SleepLogController(SleepLogService sleepLogService) {
        this.sleepLogService = sleepLogService;
    }

    @PostMapping
    public ResponseEntity<SleepLogResponse> create(@Valid @RequestBody CreateSleepLogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sleepLogService.create(request));
    }

    @GetMapping
    public ResponseEntity<SleepLogListResponse> getLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(sleepLogService.getLogs(from, to));
    }

    @PatchMapping("/{sleepLogId}")
    public ResponseEntity<SleepLogResponse> update(
            @PathVariable long sleepLogId,
            @Valid @RequestBody UpdateSleepLogRequest request) {
        return ResponseEntity.ok(sleepLogService.update(sleepLogId, request));
    }

    @DeleteMapping("/{sleepLogId}")
    public ResponseEntity<Void> delete(@PathVariable long sleepLogId) {
        sleepLogService.delete(sleepLogId);
        return ResponseEntity.noContent().build();
    }
}
