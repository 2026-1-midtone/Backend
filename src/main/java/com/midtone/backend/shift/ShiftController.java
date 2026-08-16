package com.midtone.backend.shift;

import com.midtone.backend.shift.application.schedule.ApplyShiftPatternRequest;
import com.midtone.backend.shift.application.schedule.ApplyShiftPatternResponse;
import com.midtone.backend.shift.application.schedule.BulkUpdateShiftRequest;
import com.midtone.backend.shift.application.schedule.BulkUpdateShiftResponse;
import com.midtone.backend.shift.application.schedule.CompletenessResponse;
import com.midtone.backend.shift.application.schedule.CreateShiftRequest;
import com.midtone.backend.shift.application.schedule.GetShiftsRequest;
import com.midtone.backend.shift.application.schedule.ShiftCompletenessCalculator;
import com.midtone.backend.shift.application.schedule.ShiftListResponse;
import com.midtone.backend.shift.application.schedule.ShiftPatternApplier;
import com.midtone.backend.shift.application.schedule.ShiftResponse;
import com.midtone.backend.shift.application.schedule.ShiftService;
import com.midtone.backend.shift.application.schedule.UpdateShiftRequest;
import com.midtone.backend.shift.application.schedule.UpdateShiftResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ShiftController {

    private final ShiftService shiftService;
    private final ShiftPatternApplier shiftPatternApplier;
    private final ShiftCompletenessCalculator shiftCompletenessCalculator;

    public ShiftController(
            ShiftService shiftService,
            ShiftPatternApplier shiftPatternApplier,
            ShiftCompletenessCalculator shiftCompletenessCalculator) {
        this.shiftService = shiftService;
        this.shiftPatternApplier = shiftPatternApplier;
        this.shiftCompletenessCalculator = shiftCompletenessCalculator;
    }

    @PostMapping("/shifts")
    public ResponseEntity<ShiftResponse> createShift(@Valid @RequestBody CreateShiftRequest request) {
        ShiftResponse response = shiftService.createShift(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/shifts")
    public ResponseEntity<ShiftListResponse> getShifts(@Valid @ModelAttribute GetShiftsRequest request) {
        ShiftListResponse response = shiftService.getShifts(request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/shifts/{shiftId}")
    public ResponseEntity<UpdateShiftResponse> updateShift(
            @PathVariable Long shiftId, @Valid @RequestBody UpdateShiftRequest request) {
        UpdateShiftResponse response = shiftService.updateShift(shiftId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/shifts/{shiftId}")
    public ResponseEntity<Void> deleteShift(@PathVariable Long shiftId) {
        shiftService.deleteShift(shiftId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/shifts:bulk")
    public ResponseEntity<BulkUpdateShiftResponse> bulkUpdateShifts(@Valid @RequestBody BulkUpdateShiftRequest request) {
        BulkUpdateShiftResponse response = shiftService.bulkUpdateShifts(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/shifts/pattern")
    public ResponseEntity<ApplyShiftPatternResponse> applyShiftPattern(
            @Valid @RequestBody ApplyShiftPatternRequest request) {
        ApplyShiftPatternResponse response = shiftPatternApplier.apply(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/shifts/completeness")
    public ResponseEntity<CompletenessResponse> getCompleteness(
            @RequestParam(required = false, defaultValue = "4") int weeks) {
        CompletenessResponse response = shiftCompletenessCalculator.calculate(weeks);
        return ResponseEntity.ok(response);
    }
}
