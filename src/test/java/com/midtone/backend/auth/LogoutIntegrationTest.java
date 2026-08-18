package com.midtone.backend.auth;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.auth.application.AuthService;
import com.midtone.backend.auth.application.ReissueRequest;
import com.midtone.backend.auth.application.TokenResponse;
import com.midtone.backend.auth.domain.LogoutRepository;
import com.midtone.backend.auth.domain.RefreshTokenRepository;
import com.midtone.backend.auth.jwt.JwtProvider;
import com.midtone.backend.support.IntegrationTest;
import com.midtone.backend.support.TestUserFixture;
import com.midtone.backend.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

class LogoutIntegrationTest extends IntegrationTest {

    private static final String MY_PROFILE_PATH = "/api/v1/users/me";
    private static final String LOGOUT_PATH = "/api/v1/auth/logout";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private LogoutRepository logoutRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private TestUserFixture testUserFixture;

    private User user;

    @BeforeEach
    void setUp() {
        user = testUserFixture.createUserWithSettings("logout-" + System.nanoTime());
        logoutRepository.deleteByUserId(user.getId());
        refreshTokenRepository.deleteByUserId(user.getId());
    }

    @Test
    void 로그아웃하면_기존_액세스_토큰으로는_더_이상_요청할_수_없다() throws Exception {
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), refreshToken, jwtProvider.getRefreshTokenExpiration());

        mockMvc.perform(get(MY_PROFILE_PATH).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post(LOGOUT_PATH)
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(MY_PROFILE_PATH).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 로그아웃하면_리프레시_토큰도_레디스에서_삭제된다() throws Exception {
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), refreshToken, jwtProvider.getRefreshTokenExpiration());

        mockMvc.perform(post(LOGOUT_PATH)
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());

        assertTrue(refreshTokenRepository.findByUserId(user.getId()).isEmpty());
        assertTrue(logoutRepository.findByUserId(user.getId()).isPresent());
    }

    @Test
    void 로그아웃_후_재발급받은_액세스_토큰은_다시_사용할_수_있다() throws Exception {
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), refreshToken, jwtProvider.getRefreshTokenExpiration());
        mockMvc.perform(post(LOGOUT_PATH)
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());

        String newRefreshToken = jwtProvider.createRefreshToken(user.getId());
        refreshTokenRepository.save(user.getId(), newRefreshToken, jwtProvider.getRefreshTokenExpiration());
        TokenResponse reissued = authService.reissue(new ReissueRequest(newRefreshToken));

        assertTrue(logoutRepository.findByUserId(user.getId()).isEmpty());
        mockMvc.perform(get(MY_PROFILE_PATH).header("Authorization", "Bearer " + reissued.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void 인증_없이_요청하면_401을_반환한다() throws Exception {
        mockMvc.perform(get(MY_PROFILE_PATH))
                .andExpect(status().isUnauthorized());
    }
}
