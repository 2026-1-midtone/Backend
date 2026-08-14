package com.midtone.backend.user;

import com.midtone.backend.user.application.profile.MyProfileResponse;
import com.midtone.backend.user.application.profile.UpdateProfileRequest;
import com.midtone.backend.user.application.profile.UpdateProfileResponse;
import com.midtone.backend.user.application.profile.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<MyProfileResponse> getMyProfile() {
        MyProfileResponse response = userService.getMyProfile();
        return ResponseEntity.ok(response);
    }

    @PatchMapping
    public ResponseEntity<UpdateProfileResponse> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UpdateProfileResponse response = userService.updateMyProfile(request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<Void> withdraw() {
        userService.withdraw();
        return ResponseEntity.noContent().build();
    }
}
