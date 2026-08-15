package com.midtone.backend.shift;

import com.midtone.backend.shift.application.BulkUpdateShiftRequest;
import com.midtone.backend.shift.application.BulkUpdateShiftResponse;
import com.midtone.backend.shift.application.CreateShiftRequest;
import com.midtone.backend.shift.application.GetShiftsRequest;
import com.midtone.backend.shift.application.ShiftListResponse;
import com.midtone.backend.shift.application.ShiftResponse;
import com.midtone.backend.shift.application.ShiftService;
import com.midtone.backend.shift.application.UpdateShiftRequest;
import com.midtone.backend.shift.application.UpdateShiftResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PatchMapping("/{shiftId}")
    public ResponseEntity<UpdateShiftResponse> updateShift(
            @PathVariable Long shiftId, @Valid @RequestBody UpdateShiftRequest request) {
        UpdateShiftResponse response = shiftService.updateShift(shiftId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{shiftId}")
    public ResponseEntity<Void> deleteShift(@PathVariable Long shiftId) {
        shiftService.deleteShift(shiftId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(":bulk")
    public ResponseEntity<BulkUpdateShiftResponse> bulkUpdateShifts(@Valid @RequestBody BulkUpdateShiftRequest request) {
        BulkUpdateShiftResponse response = shiftService.bulkUpdateShifts(request);
        return ResponseEntity.ok(response);
    }
}
