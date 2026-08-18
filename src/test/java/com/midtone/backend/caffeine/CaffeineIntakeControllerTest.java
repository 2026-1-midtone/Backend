package com.midtone.backend.caffeine;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.caffeine.application.CaffeineIntakeListResponse;
import com.midtone.backend.caffeine.application.CaffeineIntakeException;
import com.midtone.backend.caffeine.application.CaffeineIntakeResponse;
import com.midtone.backend.caffeine.application.CaffeineIntakeService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CaffeineIntakeController.class)
@AutoConfigureMockMvc(addFilters = false)
class CaffeineIntakeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CaffeineIntakeService caffeineIntakeService;

    @Test
    void 카페인_섭취기록을_등록한다() throws Exception {
        given(caffeineIntakeService.create(any())).willReturn(response());

        mockMvc.perform(post("/api/v1/caffeine-intakes")
                        .contentType("application/json")
                        .content("{\"consumedAt\":\"2026-08-18T09:00:00+09:00\",\"amountMg\":120,\"servings\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.intakeId").value(20))
                .andExpect(jsonPath("$.amountMg").value(120));
    }

    @Test
    void 카페인_용량이_양수가_아니면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/caffeine-intakes")
                        .contentType("application/json")
                        .content("{\"consumedAt\":\"2026-08-18T09:00:00+09:00\",\"amountMg\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("amountMg는 1 이상이어야 합니다."));
    }

    @Test
    void 기간별_카페인_기록과_합계를_조회한다() throws Exception {
        given(caffeineIntakeService.getIntakes(LocalDate.parse("2026-08-18"), LocalDate.parse("2026-08-18")))
                .willReturn(new CaffeineIntakeListResponse(
                        List.of(response()), 120, new BigDecimal("1.00")));

        mockMvc.perform(get("/api/v1/caffeine-intakes")
                        .param("from", "2026-08-18")
                        .param("to", "2026-08-18"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmountMg").value(120))
                .andExpect(jsonPath("$.totalServings").value(1.0));
    }

    @Test
    void 카페인_섭취기록을_수정한다() throws Exception {
        given(caffeineIntakeService.update(org.mockito.ArgumentMatchers.eq(20L), any())).willReturn(response());

        mockMvc.perform(patch("/api/v1/caffeine-intakes/20")
                        .contentType("application/json")
                        .content("{\"amountMg\":120}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intakeId").value(20));
    }

    @Test
    void 카페인_섭취기록을_삭제한다() throws Exception {
        mockMvc.perform(delete("/api/v1/caffeine-intakes/20"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 타인의_섭취기록은_403을_반환한다() throws Exception {
        given(caffeineIntakeService.update(org.mockito.ArgumentMatchers.eq(20L), any())).willThrow(
                new CaffeineIntakeException(CaffeineIntakeException.ErrorCode.ACCESS_DENIED));

        mockMvc.perform(patch("/api/v1/caffeine-intakes/20")
                        .contentType("application/json")
                        .content("{\"amountMg\":120}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
    }

    private CaffeineIntakeResponse response() {
        return new CaffeineIntakeResponse(
                20L,
                OffsetDateTime.parse("2026-08-18T09:00:00+09:00"),
                "Asia/Seoul",
                120,
                new BigDecimal("1.00"),
                "COFFEE",
                OffsetDateTime.parse("2026-08-18T09:01:00+09:00"),
                OffsetDateTime.parse("2026-08-18T09:01:00+09:00"));
    }
}
