package com.midtone.backend.coaching;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.coaching.application.CoachingCardDetailResponse;
import com.midtone.backend.coaching.application.CoachingException;
import com.midtone.backend.coaching.application.CoachingService;
import com.midtone.backend.coaching.application.RegenerateCoachingRequest;
import com.midtone.backend.coaching.application.RegenerateCoachingResponse;
import com.midtone.backend.coaching.application.TodayCoachingResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CoachingController.class)
@AutoConfigureMockMvc(addFilters = false)
class CoachingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CoachingService coachingService;

    @Test
    void returnsTodayCoachingForGivenDate() throws Exception {
        given(coachingService.getTodayCoaching(LocalDate.of(2026, 8, 7))).willReturn(new TodayCoachingResponse(
                1L, "2026-08-07", "NIGHT", null, false, List.of(), "본 코칭은 참고용이며 의학적 진단·치료를 대체하지 않습니다."));

        mockMvc.perform(get("/api/v1/coachings").param("date", "2026-08-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coachingDate").value("2026-08-07"))
                .andExpect(jsonPath("$.todayShiftType").value("NIGHT"));
    }

    @Test
    void returnsCardDetail() throws Exception {
        given(coachingService.getCardDetail(10L)).willReturn(new CoachingCardDetailResponse(
                10L, "NAP", "권장 낮잠", "2026-08-07T18:00:00+09:00", "2026-08-07T18:20:00+09:00", "근무 4시간 전 계산",
                new CoachingCardDetailResponse.BasedOn("NIGHT", null, "MEDIUM")));

        mockMvc.perform(get("/api/v1/coachings/cards/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardId").value(10))
                .andExpect(jsonPath("$.basedOn.caffeineSensitivity").value("MEDIUM"));
    }

    @Test
    void rejectsCardDetailWhenCardNotFound() throws Exception {
        given(coachingService.getCardDetail(999L))
                .willThrow(new CoachingException(CoachingException.ErrorCode.CARD_NOT_FOUND));

        mockMvc.perform(get("/api/v1/coachings/cards/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("해당 코칭 카드를 찾을 수 없습니다."));
    }

    @Test
    void regeneratesCoachingForRange() throws Exception {
        given(coachingService.regenerateCoaching(any(RegenerateCoachingRequest.class)))
                .willReturn(new RegenerateCoachingResponse(List.of("2026-08-05", "2026-08-06"), 2));

        mockMvc.perform(post("/api/v1/coachings:regenerate")
                        .contentType("application/json")
                        .content("{\"from\":\"2026-08-05\",\"to\":\"2026-08-06\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regeneratedCount").value(2));
    }

    @Test
    void rejectsRegenerateWithoutFromDate() throws Exception {
        mockMvc.perform(post("/api/v1/coachings:regenerate")
                        .contentType("application/json")
                        .content("{\"to\":\"2026-08-06\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("재생성 시작일은 필수 입력값입니다."));
    }
}
