package com.midtone.backend.shift;

import com.midtone.backend.shift.application.pattern.SaveShiftPatternRequest;
import com.midtone.backend.shift.application.pattern.ShiftPatternListResponse;
import com.midtone.backend.shift.application.pattern.ShiftPatternResponse;
import com.midtone.backend.shift.application.pattern.ShiftPatternService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shift-patterns")
public class ShiftPatternController {

    private final ShiftPatternService shiftPatternService;

    public ShiftPatternController(ShiftPatternService shiftPatternService) {
        this.shiftPatternService = shiftPatternService;
    }

    @GetMapping
    public ResponseEntity<ShiftPatternListResponse> getShiftPatterns() {
        ShiftPatternListResponse response = shiftPatternService.getShiftPatterns();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ShiftPatternResponse> saveShiftPattern(@Valid @RequestBody SaveShiftPatternRequest request) {
        ShiftPatternResponse response = shiftPatternService.saveShiftPattern(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{patternId}")
    public ResponseEntity<Void> deleteShiftPattern(@PathVariable Long patternId) {
        shiftPatternService.deleteShiftPattern(patternId);
        return ResponseEntity.noContent().build();
    }
}
