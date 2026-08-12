package com.midtone.backend.nap;

import com.midtone.backend.nap.application.NapService;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.ResponseEntity;
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
}
