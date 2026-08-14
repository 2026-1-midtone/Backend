package com.midtone.backend.user;

import com.midtone.backend.user.application.settings.SaveSettingsRequest;
import com.midtone.backend.user.application.settings.SettingsResponse;
import com.midtone.backend.user.application.settings.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me/settings")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    public UserSettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @GetMapping
    public ResponseEntity<SettingsResponse> getSettings() {
        SettingsResponse response = userSettingsService.getSettings();
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<SettingsResponse> saveSettings(@Valid @RequestBody SaveSettingsRequest request) {
        SettingsResponse response = userSettingsService.saveSettings(request);
        return ResponseEntity.ok(response);
    }
}
