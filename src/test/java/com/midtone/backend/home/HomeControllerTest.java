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
}
