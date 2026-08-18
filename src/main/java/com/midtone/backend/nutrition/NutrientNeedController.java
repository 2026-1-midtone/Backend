package com.midtone.backend.nutrition;

import com.midtone.backend.nutrition.application.NutrientNeedResponse;
import com.midtone.backend.nutrition.application.NutrientNeedService;
import com.midtone.backend.nutrition.application.SaveNutrientNeedsRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/nutrient-needs")
public class NutrientNeedController {
    private final NutrientNeedService service;
    public NutrientNeedController(NutrientNeedService service) { this.service = service; }
    @PutMapping public ResponseEntity<NutrientNeedResponse> replace(@Valid @RequestBody SaveNutrientNeedsRequest request) {
        return ResponseEntity.ok(service.replace(request));
    }
    @GetMapping public ResponseEntity<NutrientNeedResponse> get() { return ResponseEntity.ok(service.get()); }
    @DeleteMapping("/{nutrientCode}") public ResponseEntity<Void> delete(@PathVariable String nutrientCode) {
        service.delete(nutrientCode); return ResponseEntity.noContent().build();
    }
}
