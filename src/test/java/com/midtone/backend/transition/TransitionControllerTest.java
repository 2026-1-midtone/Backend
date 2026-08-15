package com.midtone.backend.transition;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.transition.application.TransitionException;
import com.midtone.backend.transition.application.TransitionGuideResponse;
import com.midtone.backend.transition.application.TransitionListResponse;
import com.midtone.backend.transition.application.TransitionService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TransitionController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransitionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransitionService transitionService;

    @Test
    void returnsTransitionsInRange() throws Exception {
        given(transitionService.getTransitions(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .willReturn(new TransitionListResponse(
                        List.of(new TransitionListResponse.Item("2026-08-10", "NIGHT", "DAY", "NIGHT_TO_DAY"))));

        mockMvc.perform(get("/api/v1/transitions").param("from", "2026-08-01").param("to", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transitions[0].transitionDate").value("2026-08-10"))
                .andExpect(jsonPath("$.transitions[0].protocolName").value("NIGHT_TO_DAY"));
    }

    @Test
    void returnsTransitionGuideForTransitionDay() throws Exception {
        given(transitionService.getTransitionGuide(LocalDate.of(2026, 8, 10))).willReturn(new TransitionGuideResponse(
                "2026-08-10", "NIGHT", "DAY", "NIGHT_TO_DAY", "야간에서 주간으로 전환", List.of(),
                "2026-08-10", "의학적 치료·약물 권고를 포함하지 않습니다."));

        mockMvc.perform(get("/api/v1/transitions/2026-08-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolName").value("NIGHT_TO_DAY"))
                .andExpect(jsonPath("$.disclaimer").value("의학적 치료·약물 권고를 포함하지 않습니다."));
    }

    @Test
    void rejectsTransitionGuideWhenDateIsNotATransitionDay() throws Exception {
        given(transitionService.getTransitionGuide(LocalDate.of(2026, 8, 9)))
                .willThrow(new TransitionException(TransitionException.ErrorCode.NOT_A_TRANSITION_DAY));

        mockMvc.perform(get("/api/v1/transitions/2026-08-09"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("해당 날짜는 전환일이 아닙니다."));
    }
}
