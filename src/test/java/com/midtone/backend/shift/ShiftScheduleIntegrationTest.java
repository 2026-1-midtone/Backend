package com.midtone.backend.shift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.auth.jwt.JwtProvider;
import com.midtone.backend.coaching.domain.DailyCoachingRepository;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftType;
import com.midtone.backend.support.IntegrationTest;
import com.midtone.backend.support.TestUserFixture;
import com.midtone.backend.user.domain.User;
import com.midtone.backend.user.domain.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class ShiftScheduleIntegrationTest extends IntegrationTest {

    private static final String SHIFTS_PATH = "/api/v1/shifts";
    private static final String WORK_DATE = "2026-09-01";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ShiftScheduleRepository shiftScheduleRepository;

    @Autowired
    private DailyCoachingRepository dailyCoachingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestUserFixture testUserFixture;

    private User user;
    private String authorization;

    @BeforeEach
    void setUp() {
        dailyCoachingRepository.deleteAll();
        shiftScheduleRepository.deleteAll();
        user = testUserFixture.createUserWithSettings("shift-" + System.nanoTime());
        authorization = "Bearer " + jwtProvider.createAccessToken(user.getId());
    }

    @Test
    void 같은_날짜에_일정을_두_번_등록하면_409를_반환한다() throws Exception {
        mockMvc.perform(createShiftRequest()).andExpect(status().isCreated());

        mockMvc.perform(createShiftRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("해당 날짜에 이미 근무 일정이 있습니다."));
    }

    @Test
    void 일정을_등록하면_실제로_저장된다() throws Exception {
        mockMvc.perform(createShiftRequest())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.workDate").value(WORK_DATE));

        assertEquals(1, shiftScheduleRepository.count());
        assertTrue(shiftScheduleRepository
                .findByUserIdAndWorkDate(user.getId(), LocalDate.parse(WORK_DATE))
                .isPresent());
    }

    @Test
    void 일정을_수정하면_영향받은_코칭_날짜를_함께_반환한다() throws Exception {
        String body = mockMvc.perform(createShiftRequest())
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long shiftId = shiftScheduleRepository
                .findByUserIdAndWorkDate(user.getId(), LocalDate.parse(WORK_DATE))
                .orElseThrow()
                .getId();

        mockMvc.perform(patch(SHIFTS_PATH + "/" + shiftId)
                        .header("Authorization", authorization)
                        .contentType("application/json")
                        .content("{\"shiftType\":\"DAY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedCoachingDates").isArray());

        assertTrue(body.contains(WORK_DATE));
        assertEquals(ShiftType.DAY, shiftScheduleRepository.findById(shiftId).orElseThrow().getShiftType());
    }

    @Test
    void 형식은_맞지만_존재하지_않는_날짜를_보내면_400을_반환한다() throws Exception {
        mockMvc.perform(post(SHIFTS_PATH)
                        .header("Authorization", authorization)
                        .contentType("application/json")
                        .content("{\"workDate\":\"2026-02-31\",\"shiftType\":\"NIGHT\","
                                + "\"startTime\":\"22:00\",\"endTime\":\"07:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("날짜 또는 시각 형식이 올바르지 않습니다."));
    }

    @Test
    void 회원을_탈퇴하면_근무_일정도_함께_삭제된다() throws Exception {
        mockMvc.perform(createShiftRequest()).andExpect(status().isCreated());
        assertEquals(1, shiftScheduleRepository.count());

        mockMvc.perform(delete("/api/v1/users/me").header("Authorization", authorization))
                .andExpect(status().isNoContent());

        assertTrue(userRepository.findById(user.getId()).isEmpty());
        assertEquals(0, shiftScheduleRepository.count());
    }

    private MockHttpServletRequestBuilder createShiftRequest() {
        return post(SHIFTS_PATH)
                .header("Authorization", authorization)
                .contentType("application/json")
                .content("{\"workDate\":\"" + WORK_DATE + "\",\"shiftType\":\"NIGHT\","
                        + "\"startTime\":\"22:00\",\"endTime\":\"07:00\"}");
    }
}
