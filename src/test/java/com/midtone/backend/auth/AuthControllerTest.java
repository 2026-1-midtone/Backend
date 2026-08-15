package com.midtone.backend.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.auth.application.AuthService;
import com.midtone.backend.auth.application.GoogleLoginRequest;
import com.midtone.backend.auth.application.LoginResponse;
import com.midtone.backend.auth.application.ReissueRequest;
import com.midtone.backend.auth.application.TokenResponse;
import com.midtone.backend.user.application.profile.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void loginsWithGoogleAndReturnsTokensAndUser() throws Exception {
        UserResponse user = new UserResponse(1L, "user@test.com", "닉네임", null, "Asia/Seoul");
        given(authService.loginWithGoogle(any(GoogleLoginRequest.class)))
                .willReturn(new LoginResponse("access-token", "refresh-token", true, user, true));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType("application/json")
                        .content("{\"idToken\":\"valid-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.isNewUser").value(true))
                .andExpect(jsonPath("$.user.email").value("user@test.com"));
    }

    @Test
    void rejectsGoogleLoginWithoutIdToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("idToken은 필수 입력값입니다."));
    }

    @Test
    void reissuesAccessAndRefreshTokens() throws Exception {
        given(authService.reissue(any(ReissueRequest.class)))
                .willReturn(new TokenResponse("new-access-token", "new-refresh-token"));

        mockMvc.perform(post("/api/v1/auth/reissue")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"valid-refresh-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void rejectsReissueWithInvalidRefreshToken() throws Exception {
        given(authService.reissue(any(ReissueRequest.class)))
                .willThrow(new AuthException(AuthException.ErrorCode.INVALID_REFRESH_TOKEN));

        mockMvc.perform(post("/api/v1/auth/reissue")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"expired-token\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("만료된 리프레시 토큰입니다. 다시 로그인해 주세요."));
    }

    @Test
    void logsOutAndReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"valid-refresh-token\"}"))
                .andExpect(status().isNoContent());
    }
}
