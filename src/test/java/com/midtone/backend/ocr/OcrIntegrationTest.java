package com.midtone.backend.ocr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.auth.jwt.JwtProvider;
import com.midtone.backend.coaching.domain.DailyCoachingRepository;
import com.midtone.backend.ocr.documentai.DocumentAiClient;
import com.midtone.backend.ocr.domain.OcrDraftShiftRepository;
import com.midtone.backend.ocr.domain.OcrJobRepository;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftSource;
import com.midtone.backend.shift.domain.ShiftType;
import com.midtone.backend.support.IntegrationTest;
import com.midtone.backend.support.TestUserFixture;
import com.midtone.backend.user.domain.User;
import java.io.InputStream;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class OcrIntegrationTest extends IntegrationTest {

    private static final String OCR_JOBS_PATH = "/api/v1/ocr/jobs";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private OcrJobRepository ocrJobRepository;

    @Autowired
    private OcrDraftShiftRepository ocrDraftShiftRepository;

    @Autowired
    private ShiftScheduleRepository shiftScheduleRepository;

    @Autowired
    private DailyCoachingRepository dailyCoachingRepository;

    @Autowired
    private TestUserFixture testUserFixture;

    @MockitoBean
    private DocumentAiClient documentAiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User user;
    private String authorization;
    private JsonNode fixtureDocument;

    @BeforeEach
    void setUp() throws Exception {
        ocrDraftShiftRepository.deleteAll();
        ocrJobRepository.deleteAll();
        dailyCoachingRepository.deleteAll();
        shiftScheduleRepository.deleteAll();
        user = testUserFixture.createUserWithSettings("ocr-" + System.nanoTime());
        authorization = "Bearer " + jwtProvider.createAccessToken(user.getId());
        try (InputStream in = getClass().getResourceAsStream("/ocr/form-parser-response.json")) {
            fixtureDocument = objectMapper.readTree(in);
        }
    }

    private long uploadJob() throws Exception {
        MvcResult result = mockMvc.perform(multipart(OCR_JOBS_PATH)
                        .file(new MockMultipartFile("image", "roster.png", "image/png", new byte[] {1, 2, 3}))
                        .param("month", "2026-08")
                        .header("Authorization", authorization))
                .andExpect(status().isAccepted())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("jobId").asLong();
    }

    private JsonNode pollUntilStatus(long jobId, String expectedStatus) throws Exception {
        JsonNode body = null;
        for (int i = 0; i < 25; i++) {
            MvcResult result = mockMvc.perform(get(OCR_JOBS_PATH + "/" + jobId)
                            .header("Authorization", authorization))
                    .andExpect(status().isOk())
                    .andReturn();
            body = objectMapper.readTree(result.getResponse().getContentAsString());
            if (expectedStatus.equals(body.path("status").asText())) {
                return body;
            }
            Thread.sleep(200);
        }
        fail("잡이 " + expectedStatus + " 상태에 도달하지 않았습니다. 마지막 응답: " + body);
        return null;
    }

    @Test
    void 업로드부터_검수_확정까지_전체_흐름이_동작한다() throws Exception {
        given(documentAiClient.process(any(), any())).willReturn(fixtureDocument);

        // 덮어쓰기 검증용 기존 일정 (2026-08-02 DAY)
        mockMvc.perform(post("/api/v1/shifts")
                        .contentType("application/json")
                        .content("{\"workDate\":\"2026-08-02\",\"shiftType\":\"DAY\"}")
                        .header("Authorization", authorization))
                .andExpect(status().isCreated());

        long jobId = uploadJob();
        JsonNode completed = pollUntilStatus(jobId, "COMPLETED");
        assertEquals(4, completed.path("drafts").size());
        long firstDraftId = completed.path("drafts").get(0).path("draftId").asLong();

        // 첫 초안(2026-08-01 DAY)을 NIGHT로 보정
        mockMvc.perform(patch(OCR_JOBS_PATH + "/" + jobId + "/drafts/" + firstDraftId)
                        .contentType("application/json")
                        .content("{\"shiftType\":\"NIGHT\"}")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shiftType").value("NIGHT"));

        // 확정: 4건 반영, 2026-08-02는 덮어쓰기
        mockMvc.perform(post(OCR_JOBS_PATH + "/" + jobId + ":confirm")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedCount").value(4))
                .andExpect(jsonPath("$.replacedDates[0]").value("2026-08-02"));

        ShiftSchedule saved = shiftScheduleRepository
                .findByUserIdAndWorkDate(user.getId(), LocalDate.of(2026, 8, 1))
                .orElseThrow();
        assertEquals(ShiftType.NIGHT, saved.getShiftType());
        assertEquals(ShiftSource.OCR, saved.getSource());
        assertTrue(saved.isConfirmed());

        mockMvc.perform(get(OCR_JOBS_PATH + "/" + jobId).header("Authorization", authorization))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void Document_AI_실패_시_FAILED가_되고_재시도로_복구한다() throws Exception {
        given(documentAiClient.process(any(), any()))
                .willThrow(new DocumentAiClient.DocumentAiCallException("boom", null));

        long jobId = uploadJob();
        JsonNode failed = pollUntilStatus(jobId, "FAILED");
        assertNotNull(failed.path("errorMessage").asText(null));

        // 예외를 던지는 목을 재스텁할 때는 willReturn().given() 형태를 써야 스텁 중 호출되지 않는다
        org.mockito.BDDMockito.willReturn(fixtureDocument).given(documentAiClient).process(any(), any());

        mockMvc.perform(post(OCR_JOBS_PATH + "/" + jobId + ":retry")
                        .header("Authorization", authorization))
                .andExpect(status().isAccepted());

        pollUntilStatus(jobId, "COMPLETED");
    }

    @Test
    void 다른_사용자의_잡에_접근하면_403을_반환한다() throws Exception {
        given(documentAiClient.process(any(), any())).willReturn(fixtureDocument);
        long jobId = uploadJob();

        User other = testUserFixture.createUserWithSettings("ocr-other-" + System.nanoTime());
        String otherAuthorization = "Bearer " + jwtProvider.createAccessToken(other.getId());

        mockMvc.perform(get(OCR_JOBS_PATH + "/" + jobId).header("Authorization", otherAuthorization))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."));
    }
}
