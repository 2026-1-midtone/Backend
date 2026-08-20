package com.midtone.backend.routine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.auth.jwt.JwtProvider;
import com.midtone.backend.coaching.domain.CoachingCardRepository;
import com.midtone.backend.coaching.domain.DailyCoachingRepository;
import com.midtone.backend.routine.domain.RoutineTaskRepository;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.support.IntegrationTest;
import com.midtone.backend.support.TestUserFixture;
import com.midtone.backend.user.domain.User;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class RoutineGenerationIntegrationTest extends IntegrationTest {

    private static final String OFFSET_DATE_TIME = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?\\+09:00";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private RoutineTaskRepository routineTaskRepository;

    @Autowired
    private CoachingCardRepository coachingCardRepository;

    @Autowired
    private DailyCoachingRepository dailyCoachingRepository;

    @Autowired
    private ShiftScheduleRepository shiftScheduleRepository;

    @Autowired
    private TestUserFixture testUserFixture;

    private String authorization;

    @BeforeEach
    void setUp() {
        routineTaskRepository.deleteAll();
        coachingCardRepository.deleteAll();
        dailyCoachingRepository.deleteAll();
        shiftScheduleRepository.deleteAll();
        User user = testUserFixture.createUserWithSettings("routine-gen-" + System.nanoTime());
        authorization = "Bearer " + jwtProvider.createAccessToken(user.getId());
    }

    @Test
    void 코칭을_조회하면_코칭_카드_기반_루틴이_생성된다() throws Exception {
        createShift("2026-09-10", "NIGHT", "22:00", "07:00");

        mockMvc.perform(get("/api/v1/coachings").param("date", "2026-09-10")
                        .header("Authorization", authorization))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/routines").param("date", "2026-09-10")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks", Matchers.not(Matchers.empty())))
                .andExpect(jsonPath("$.tasks[*].sourceType", Matchers.hasItem("COACHING")));
    }

    @Test
    void 루틴_작업은_권장_시간창을_함께_내려준다() throws Exception {
        createShift("2026-09-10", "NIGHT", "22:00", "07:00");

        mockMvc.perform(get("/api/v1/coachings").param("date", "2026-09-10")
                        .header("Authorization", authorization))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/routines").param("date", "2026-09-10")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks[0].windowStart", Matchers.matchesRegex(OFFSET_DATE_TIME)))
                .andExpect(jsonPath("$.tasks[0].windowEnd", Matchers.matchesRegex(OFFSET_DATE_TIME)));
    }

    @Test
    void 전환일_코칭을_조회하면_전환_프로토콜_루틴이_추가된다() throws Exception {
        createShift("2026-09-09", "NIGHT", "22:00", "07:00");
        createShift("2026-09-10", "DAY", "07:00", "15:00");

        mockMvc.perform(get("/api/v1/coachings").param("date", "2026-09-10")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isTransitionDay").value(true));

        mockMvc.perform(get("/api/v1/routines").param("date", "2026-09-10")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks[*].sourceType", Matchers.hasItem("TRANSITION")));
    }

    @Test
    void 코칭을_재생성하면_루틴이_중복되지_않는다() throws Exception {
        createShift("2026-09-10", "NIGHT", "22:00", "07:00");
        mockMvc.perform(get("/api/v1/coachings").param("date", "2026-09-10")
                        .header("Authorization", authorization))
                .andExpect(status().isOk());
        long countAfterFirst = routineTaskRepository.count();

        mockMvc.perform(post("/api/v1/coachings:regenerate")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"from\":\"2026-09-10\",\"to\":\"2026-09-10\"}"))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertEquals(countAfterFirst, routineTaskRepository.count());
    }

    private void createShift(String workDate, String shiftType, String startTime, String endTime) throws Exception {
        mockMvc.perform(post("/api/v1/shifts")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workDate\":\"%s\",\"shiftType\":\"%s\",\"startTime\":\"%s\",\"endTime\":\"%s\"}"
                                .formatted(workDate, shiftType, startTime, endTime)))
                .andExpect(status().isCreated());
    }
}
