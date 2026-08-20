package com.midtone.backend.global.error;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.auth.AuthController;
import com.midtone.backend.auth.application.AuthService;
import com.midtone.backend.auth.application.GoogleLoginRequest;
import com.midtone.backend.ocr.OcrController;
import com.midtone.backend.ocr.application.OcrJobService;
import com.midtone.backend.transition.TransitionController;
import com.midtone.backend.transition.application.TransitionService;
import java.time.format.DateTimeParseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@WebMvcTest({AuthController.class, OcrController.class, TransitionController.class})
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private OcrJobService ocrJobService;

    @MockitoBean
    private TransitionService transitionService;

    @Test
    void returnsGenericMessageForUnexpectedException() throws Exception {
        given(authService.loginWithGoogle(any(GoogleLoginRequest.class)))
                .willThrow(new NullPointerException("unexpected"));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType("application/json")
                        .content("{\"idToken\":\"valid-id-token\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
    }

    @Test
    void returnsNotFoundForUnmappedPath() throws Exception {
        mockMvc.perform(get("/api/v1/__does-not-exist__"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("요청한 리소스를 찾을 수 없습니다."));
    }

    @Test
    void returnsBadRequestForUnparsableDate() throws Exception {
        given(authService.loginWithGoogle(any(GoogleLoginRequest.class)))
                .willThrow(new DateTimeParseException("invalid", "2026-02-31", 0));

        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType("application/json")
                        .content("{\"idToken\":\"valid-id-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("날짜 또는 시각 형식이 올바르지 않습니다."));
    }

    @Test
    void returnsBadRequestWhenRequiredMultipartPartIsMissing() throws Exception {
        mockMvc.perform(multipart("/api/v1/ocr/jobs").param("month", "2026-08"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("image 항목은 필수입니다."));
    }

    @Test
    void returnsBadRequestWhenRequiredParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/transitions").param("from", "2026-08-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("to 파라미터는 필수입니다."));
    }

    @Test
    void returnsBadRequestWhenParameterTypeDoesNotMatch() throws Exception {
        mockMvc.perform(get("/api/v1/transitions").param("from", "어제").param("to", "2026-08-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from 값이 올바르지 않습니다."));
    }

    @Test
    void returnsBadRequestForUnreadableRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType("application/json")
                        .content("{\"idToken\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("요청 본문을 읽을 수 없습니다."));
    }

    @Test
    void returnsUnsupportedMediaTypeForUnsupportedContentType() throws Exception {
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType("text/plain")
                        .content("idToken=valid-id-token"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("지원하지 않는 Content-Type입니다."));
    }

    @Test
    void returnsMethodNotAllowedForUnsupportedMethod() throws Exception {
        mockMvc.perform(get("/api/v1/auth/google"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("지원하지 않는 HTTP 메서드입니다."));
    }

    @Test
    void returnsPayloadTooLargeWhenUploadExceedsLimit() throws Exception {
        given(ocrJobService.upload(any(), any()))
                .willThrow(new MaxUploadSizeExceededException(10L * 1024 * 1024));

        mockMvc.perform(multipart("/api/v1/ocr/jobs")
                        .file(new MockMultipartFile("image", "roster.png", "image/png", new byte[] {1})))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.message").value("업로드할 수 있는 최대 용량을 초과했습니다."));
    }
}
