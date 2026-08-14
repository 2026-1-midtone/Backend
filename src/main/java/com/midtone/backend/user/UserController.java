package com.midtone.backend.user;

import com.midtone.backend.user.application.MyProfileResponse;
import com.midtone.backend.user.application.UpdateProfileRequest;
import com.midtone.backend.user.application.UpdateProfileResponse;
import com.midtone.backend.user.application.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
}
