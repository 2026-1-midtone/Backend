package com.midtone.backend.sleep;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.sleep.application.SleepLogListResponse;
import com.midtone.backend.sleep.application.SleepLogException;
import com.midtone.backend.sleep.application.SleepLogResponse;
import com.midtone.backend.sleep.application.SleepLogService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SleepLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class SleepLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SleepLogService sleepLogService;

    @Test
    void 수면기록을_등록한다() throws Exception {
        given(sleepLogService.create(any())).willReturn(response());

        mockMvc.perform(post("/api/v1/sleep-logs")
                        .contentType("application/json")
                        .content("{\"sleptAt\":\"2026-08-17T23:30:00+09:00\",\"wokeAt\":\"2026-08-18T07:10:00+09:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sleepLogId").value(10))
                .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    void 취침시각이_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/sleep-logs")
                        .contentType("application/json")
                        .content("{\"wokeAt\":\"2026-08-18T07:10:00+09:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("sleptAt은 필수 입력값입니다."));
    }

    @Test
    void 기간별_수면기록을_조회한다() throws Exception {
        given(sleepLogService.getLogs(LocalDate.parse("2026-08-17"), LocalDate.parse("2026-08-18")))
                .willReturn(new SleepLogListResponse(List.of(response())));

        mockMvc.perform(get("/api/v1/sleep-logs")
                        .param("from", "2026-08-17")
                        .param("to", "2026-08-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logs[0].sleepLogId").value(10));
    }

    @Test
    void 수면기록을_수정한다() throws Exception {
        given(sleepLogService.update(org.mockito.ArgumentMatchers.eq(10L), any())).willReturn(response());

        mockMvc.perform(patch("/api/v1/sleep-logs/10")
                        .contentType("application/json")
                        .content("{\"source\":\"MANUAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sleepLogId").value(10));
    }

    @Test
    void 수면기록을_삭제한다() throws Exception {
        mockMvc.perform(delete("/api/v1/sleep-logs/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 겹치는_수면기록은_409를_반환한다() throws Exception {
        given(sleepLogService.create(any())).willThrow(
                new SleepLogException(SleepLogException.ErrorCode.OVERLAPPING_LOG));

        mockMvc.perform(post("/api/v1/sleep-logs")
                        .contentType("application/json")
                        .content("{\"sleptAt\":\"2026-08-17T23:30:00+09:00\",\"wokeAt\":\"2026-08-18T07:10:00+09:00\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("기존 수면 기록과 시간이 겹칩니다."));
    }

    private SleepLogResponse response() {
        return new SleepLogResponse(
                10L,
                OffsetDateTime.parse("2026-08-17T23:30:00+09:00"),
                OffsetDateTime.parse("2026-08-18T07:10:00+09:00"),
                "Asia/Seoul",
                "MANUAL",
                OffsetDateTime.parse("2026-08-18T07:11:00+09:00"),
                OffsetDateTime.parse("2026-08-18T07:11:00+09:00"));
    }
}
