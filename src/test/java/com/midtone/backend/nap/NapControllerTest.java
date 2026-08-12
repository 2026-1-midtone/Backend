package com.midtone.backend.nap;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.nap.application.NapService;
import com.midtone.backend.nap.application.NapService.ActiveNap;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NapController.class)
@AutoConfigureMockMvc(addFilters = false)
class NapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NapService napService;

    @Test
    void returnsActiveNapForLocalDevelopmentUser() throws Exception {
        given(napService.getActiveNap()).willReturn(new ActiveNap(
                44L,
                20,
                OffsetDateTime.parse("2026-08-07T18:00:00+09:00"),
                OffsetDateTime.parse("2026-08-07T18:20:00+09:00"),
                720,
                "RUNNING"));

        mockMvc.perform(get("/api/v1/naps/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.napId").value(44))
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void returnsNullActiveNapWhenNoNapIsRunning() throws Exception {
        given(napService.getActiveNap()).willReturn(null);

        mockMvc.perform(get("/api/v1/naps/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeNap").isEmpty());
    }

    @Test
    void startsNapWithRequestedDuration() throws Exception {
        given(napService.startNap(25)).willReturn(new ActiveNap(
                45L,
                25,
                OffsetDateTime.parse("2026-08-07T18:00:00+09:00"),
                OffsetDateTime.parse("2026-08-07T18:25:00+09:00"),
                1500,
                "RUNNING"));

        mockMvc.perform(post("/api/v1/naps")
                        .contentType("application/json")
                        .content("{\"plannedMinutes\":25}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.napId").value(45))
                .andExpect(jsonPath("$.plannedMinutes").value(25))
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }
}
