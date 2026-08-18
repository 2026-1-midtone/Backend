package com.midtone.backend.nutrition;

import com.midtone.backend.nutrition.application.NutritionContentService;
import com.midtone.backend.nutrition.application.NutritionResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class NutritionContentController {
    private final NutritionContentService service;
    public NutritionContentController(NutritionContentService service) { this.service = service; }

    @GetMapping("/nutrition-contents")
    public ResponseEntity<NutritionResponses.ListResponse> list(@RequestParam(required = false) String timingTag,
            @RequestParam(required = false) String contentType, @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(service.list(timingTag, contentType, page));
    }
    @GetMapping("/nutrition-contents/{contentId}")
    public ResponseEntity<NutritionResponses.Detail> detail(@PathVariable long contentId) {
        return ResponseEntity.ok(service.detail(contentId));
    }
    @PostMapping("/nutrition-contents/{contentId}/favorite")
    public ResponseEntity<NutritionResponses.Favorite> addFavorite(@PathVariable long contentId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addFavorite(contentId));
    }
    @DeleteMapping("/nutrition-contents/{contentId}/favorite")
    public ResponseEntity<Void> removeFavorite(@PathVariable long contentId) {
        service.removeFavorite(contentId); return ResponseEntity.noContent().build();
    }
    @GetMapping("/users/me/favorites")
    public ResponseEntity<NutritionResponses.FavoritesResponse> favorites(@RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(service.favorites(page));
    }
}
