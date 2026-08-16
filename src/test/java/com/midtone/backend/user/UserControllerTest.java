package com.midtone.backend.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.user.application.profile.MyProfileResponse;
import com.midtone.backend.user.application.profile.UpdateProfileRequest;
import com.midtone.backend.user.application.profile.UpdateProfileResponse;
import com.midtone.backend.user.application.profile.UserService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void returnsMyProfile() throws Exception {
        given(userService.getMyProfile()).willReturn(new MyProfileResponse(
                1L, "user@test.com", "닉네임", null, "Asia/Seoul", LocalDateTime.of(2026, 1, 1, 0, 0)));

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.nickname").value("닉네임"));
    }

    @Test
    void updatesMyProfile() throws Exception {
        given(userService.updateMyProfile(any(UpdateProfileRequest.class)))
                .willReturn(new UpdateProfileResponse(1L, "새닉네임", "America/New_York"));

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType("application/json")
                        .content("{\"nickname\":\"새닉네임\",\"timezone\":\"America/New_York\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("새닉네임"))
                .andExpect(jsonPath("$.timezone").value("America/New_York"));
    }

    @Test
    void rejectsProfileUpdateWithTooLongNickname() throws Exception {
        String tooLongNickname = "닉".repeat(51);

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType("application/json")
                        .content("{\"nickname\":\"" + tooLongNickname + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("닉네임은 50자를 초과할 수 없습니다."));
    }

    @Test
    void rejectsProfileUpdateWithInvalidTimezone() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType("application/json")
                        .content("{\"timezone\":\"Not/AZone\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("타임존은 Asia/Seoul과 같은 유효한 값이어야 합니다."));
    }

    @Test
    void withdrawsAndReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me"))
                .andExpect(status().isNoContent());
    }
}
