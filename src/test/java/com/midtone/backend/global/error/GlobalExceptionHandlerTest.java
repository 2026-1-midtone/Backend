package com.midtone.backend.global.error;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.auth.AuthController;
import com.midtone.backend.auth.application.AuthService;
import com.midtone.backend.auth.application.GoogleLoginRequest;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void returnsGenericMessageForUnexpectedException() throws Exception {
        given(authService.loginWithGoogle(any(GoogleLoginRequest.class)))
                .willThrow(new NullPointerException("unexpected"));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType("application/json")
                        .content("{\"idToken\":\"valid-id-token\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
    }

    @Test
    void returnsNotFoundForUnmappedPath() throws Exception {
        mockMvc.perform(get("/api/v1/__does-not-exist__"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."));
    }

    @Test
    void returnsBadRequestForUnparsableDate() throws Exception {
        given(authService.loginWithGoogle(any(GoogleLoginRequest.class)))
                .willThrow(new DateTimeParseException("invalid", "2026-02-31", 0));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType("application/json")
                        .content("{\"idToken\":\"valid-id-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("날짜 또는 시각 형식이 올바르지 않습니다."));
    }
}
