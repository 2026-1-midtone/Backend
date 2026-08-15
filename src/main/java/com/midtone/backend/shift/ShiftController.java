package com.midtone.backend.shift;

import com.midtone.backend.shift.application.CreateShiftRequest;
import com.midtone.backend.shift.application.GetShiftsRequest;
import com.midtone.backend.shift.application.ShiftListResponse;
import com.midtone.backend.shift.application.ShiftResponse;
import com.midtone.backend.shift.application.ShiftService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shifts")
public class ShiftController {

    private final ShiftService shiftService;

    public ShiftController(ShiftService shiftService) {
        this.shiftService = shiftService;
    }

    @PostMapping
    public ResponseEntity<ShiftResponse> createShift(@Valid @RequestBody CreateShiftRequest request) {
        ShiftResponse response = shiftService.createShift(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ShiftListResponse> getShifts(@Valid @ModelAttribute GetShiftsRequest request) {
        ShiftListResponse response = shiftService.getShifts(request);
        return ResponseEntity.ok(response);
    }
}
