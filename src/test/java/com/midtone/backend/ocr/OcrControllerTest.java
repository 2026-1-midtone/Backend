package com.midtone.backend.ocr;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.ocr.application.ConfirmOcrJobResponse;
import com.midtone.backend.ocr.application.OcrDraftResponse;
import com.midtone.backend.ocr.application.OcrException;
import com.midtone.backend.ocr.application.OcrJobDetailResponse;
import com.midtone.backend.ocr.application.OcrJobResponse;
import com.midtone.backend.ocr.application.OcrJobService;
import com.midtone.backend.ocr.application.UpdateOcrDraftRequest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OcrController.class)
@AutoConfigureMockMvc(addFilters = false)
class OcrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OcrJobService ocrJobService;

    @Test
    void 이미지를_업로드하면_202와_잡_ID를_반환한다() throws Exception {
        given(ocrJobService.upload(any(), eq("2026-08"))).willReturn(new OcrJobResponse(1L, "PENDING"));

        mockMvc.perform(multipart("/api/v1/ocr/jobs")
                        .file(new MockMultipartFile("image", "roster.png", "image/png", new byte[] {1}))
                        .param("month", "2026-08"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void 잡_상태를_조회한다() throws Exception {
        given(ocrJobService.getJob(1L)).willReturn(new OcrJobDetailResponse(
                1L, "COMPLETED", "2026-08", null,
                List.of(new OcrDraftResponse(5L, "2026-08-01", "DAY", null, null, new BigDecimal("0.970"), false))));

        mockMvc.perform(get("/api/v1/ocr/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.drafts[0].draftId").value(5))
                .andExpect(jsonPath("$.drafts[0].shiftType").value("DAY"));
    }

    @Test
    void 초안을_보정한다() throws Exception {
        given(ocrJobService.updateDraft(eq(1L), eq(5L), any(UpdateOcrDraftRequest.class)))
                .willReturn(new OcrDraftResponse(
                        5L, "2026-08-01", "NIGHT", null, null, new BigDecimal("0.970"), false));

        mockMvc.perform(patch("/api/v1/ocr/jobs/1/drafts/5")
                        .contentType("application/json")
                        .content("{\"shiftType\":\"NIGHT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shiftType").value("NIGHT"));
    }

    @Test
    void 확정하면_반영_결과를_반환한다() throws Exception {
        given(ocrJobService.confirm(1L)).willReturn(
                new ConfirmOcrJobResponse(3, List.of("2026-08-01"), List.of("2026-08-01", "2026-08-02"), List.of()));

        mockMvc.perform(post("/api/v1/ocr/jobs/1:confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedCount").value(3))
                .andExpect(jsonPath("$.replacedDates[0]").value("2026-08-01"));
    }

    @Test
    void 재시도하면_202를_반환한다() throws Exception {
        given(ocrJobService.retry(1L)).willReturn(new OcrJobResponse(1L, "PROCESSING"));

        mockMvc.perform(post("/api/v1/ocr/jobs/1:retry"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void 완료되지_않은_잡을_확정하면_409를_반환한다() throws Exception {
        given(ocrJobService.confirm(anyLong()))
                .willThrow(new OcrException(OcrException.ErrorCode.JOB_NOT_COMPLETED));

        mockMvc.perform(post("/api/v1/ocr/jobs/1:confirm"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("분석이 완료된 작업만 검수·확정할 수 있습니다."));
    }
}
