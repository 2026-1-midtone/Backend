package com.midtone.backend.shift;

import com.midtone.backend.shift.application.schedule.SaveShiftTimeDefaultsRequest;
import com.midtone.backend.shift.application.schedule.ShiftTimeDefaultService;
import com.midtone.backend.shift.application.schedule.ShiftTimeDefaultsResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/shift-time-defaults")
public class ShiftTimeDefaultController {

    private final ShiftTimeDefaultService shiftTimeDefaultService;

    public ShiftTimeDefaultController(ShiftTimeDefaultService shiftTimeDefaultService) {
        this.shiftTimeDefaultService = shiftTimeDefaultService;
    }

    @GetMapping
    public ResponseEntity<ShiftTimeDefaultsResponse> get() {
        return ResponseEntity.ok(shiftTimeDefaultService.get());
    }

    @PutMapping
    public ResponseEntity<ShiftTimeDefaultsResponse> replace(
            @Valid @RequestBody SaveShiftTimeDefaultsRequest request) {
        return ResponseEntity.ok(shiftTimeDefaultService.replace(request));
    }
}
