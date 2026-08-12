package com.midtone.backend.nap;

import com.midtone.backend.nap.application.NapService;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/naps")
public class NapController {

    private final NapService napService;

    public NapController(NapService napService) {
        this.napService = napService;
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveNap() {
        NapService.ActiveNap activeNap = napService.getActiveNap();
        if (activeNap == null) {
            return ResponseEntity.ok(Collections.singletonMap("activeNap", null));
        }
        return ResponseEntity.ok(activeNap);
    }

    @org.springframework.web.bind.annotation.PostMapping
    public ResponseEntity<NapService.ActiveNap> startNap(@RequestBody(required = false) StartNapRequest request) {
        Integer plannedMinutes = request == null ? null : request.plannedMinutes();
        return ResponseEntity.status(201).body(napService.startNap(plannedMinutes));
    }

    @PatchMapping("/{napId}")
    public NapService.FinishedNap finishNap(@PathVariable long napId, @RequestBody FinishNapRequest request) {
        return napService.finishNap(napId, request.status());
    }

    public record StartNapRequest(Integer plannedMinutes) {
    }

    public record FinishNapRequest(String status) {
    }
}
