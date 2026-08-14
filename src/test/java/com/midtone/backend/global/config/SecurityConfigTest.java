package com.midtone.backend.global.config;

import com.midtone.backend.auth.application.AuthService;
import com.midtone.backend.auth.jwt.JwtProvider;
import com.midtone.backend.nap.application.NapService;
import com.midtone.backend.routine.application.RoutineService;
import com.midtone.backend.user.application.UserService;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NapService napService;

    @MockitoBean
    private RoutineService routineService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserService userService;

    @Test
    void unauthenticatedApiRequestReturnsJsonUnauthorizedResponse() throws Exception {
        mockMvc.perform(get("/api/v1/shifts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    void validAccessTokenPassesAuthenticationAndReachesDispatcher() throws Exception {
        when(jwtProvider.isValid("valid-token")).thenReturn(true);
        when(jwtProvider.isAccessToken("valid-token")).thenReturn(true);
        when(jwtProvider.getUserId("valid-token")).thenReturn(1L);

        mockMvc.perform(get("/api/v1/shifts").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }
}
