package com.midtone.backend.home;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.home.application.HomeDashboardResponse;
import com.midtone.backend.home.application.HomeService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HomeService homeService;

    @Test
    void returnsHomeDashboard() throws Exception {
        HomeDashboardResponse response = new HomeDashboardResponse(
                "2026-08-09",
                new HomeDashboardResponse.TodayShift("NIGHT", "22:00", "07:00"),
                null,
                false,
                new HomeDashboardResponse.RoutineProgress(3, 1, 0.33),
                List.of(),
                new HomeDashboardResponse.Streak(2),
                null,
                null);
        given(homeService.getDashboard()).willReturn(response);

        mockMvc.perform(get("/api/v1/home/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-08-09"))
                .andExpect(jsonPath("$.todayShift.shiftType").value("NIGHT"))
                .andExpect(jsonPath("$.routineProgress.total").value(3))
                .andExpect(jsonPath("$.streak.currentStreak").value(2));
    }

    @Test
    void 다음_근무_시작까지_남은_시간은_startInMinutes로_내려준다() throws Exception {
        HomeDashboardResponse response = new HomeDashboardResponse(
                "2026-08-09",
                null,
                new HomeDashboardResponse.NextShift("2026-08-10", "DAY", 540),
                false,
                new HomeDashboardResponse.RoutineProgress(0, 0, 0.0),
                List.of(),
                new HomeDashboardResponse.Streak(0),
                null,
                null);
        given(homeService.getDashboard()).willReturn(response);

        mockMvc.perform(get("/api/v1/home/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextShift.startInMinutes").value(540));
    }

    @Test
    void 홈_코칭_카드는_창_종료_시각까지_내려준다() throws Exception {
        HomeDashboardResponse response = new HomeDashboardResponse(
                "2026-08-09",
                null,
                null,
                false,
                new HomeDashboardResponse.RoutineProgress(0, 0, 0.0),
                List.of(new HomeDashboardResponse.TopCoachingCard(
                        7L, "NAP", "권장 낮잠",
                        "2026-08-09T13:00+09:00", "2026-08-09T14:30+09:00")),
                new HomeDashboardResponse.Streak(0),
                null,
                null);
        given(homeService.getDashboard()).willReturn(response);

        mockMvc.perform(get("/api/v1/home/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topCoachingCards[0].windowStart").value("2026-08-09T13:00+09:00"))
                .andExpect(jsonPath("$.topCoachingCards[0].windowEnd").value("2026-08-09T14:30+09:00"));
    }
}
