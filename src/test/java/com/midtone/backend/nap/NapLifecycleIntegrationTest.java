package com.midtone.backend.nap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.auth.jwt.JwtProvider;
import com.midtone.backend.nap.domain.NapSessionRepository;
import com.midtone.backend.nap.domain.NapStatus;
import com.midtone.backend.support.IntegrationTest;
import com.midtone.backend.support.TestUserFixture;
import com.midtone.backend.user.domain.User;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class NapLifecycleIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private NapSessionRepository napSessionRepository;

    @Autowired
    private TestUserFixture testUserFixture;

    private String authorization;

    @BeforeEach
    void setUp() {
        napSessionRepository.deleteAll();
        User user = testUserFixture.createUserWithSettings("nap-lifecycle-" + System.nanoTime());
        authorization = "Bearer " + jwtProvider.createAccessToken(user.getId());
    }

    @Test
    void 낮잠을_완료하면_완료_상태가_저장된다() throws Exception {
        long napId = startNap();

        mockMvc.perform(patch("/api/v1/naps/" + napId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        assertThat(napSessionRepository.findById(napId).orElseThrow().getStatus())
                .isEqualTo(NapStatus.COMPLETED);
    }

    @Test
    void 낮잠을_완료하면_진행_중인_낮잠이_사라진다() throws Exception {
        long napId = startNap();
        finishNap(napId);

        mockMvc.perform(get("/api/v1/naps/active").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeNap").doesNotExist());
    }

    @Test
    void 낮잠을_완료하면_새_낮잠을_시작할_수_있다() throws Exception {
        finishNap(startNap());

        mockMvc.perform(post("/api/v1/naps")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plannedMinutes\":20}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    private long startNap() throws Exception {
        String response = mockMvc.perform(post("/api/v1/naps")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"plannedMinutes\":20}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(response, "$.napId")).longValue();
    }

    private void finishNap(long napId) throws Exception {
        mockMvc.perform(patch("/api/v1/naps/" + napId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());
    }
}
