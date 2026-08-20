package com.midtone.backend.routine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.midtone.backend.auth.jwt.JwtProvider;
import com.midtone.backend.coaching.domain.CoachingCardRepository;
import com.midtone.backend.coaching.domain.DailyCoachingRepository;
import com.midtone.backend.routine.domain.RoutineTaskRepository;
import com.midtone.backend.routine.domain.TaskStatus;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.support.IntegrationTest;
import com.midtone.backend.support.TestUserFixture;
import com.midtone.backend.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 루틴 항목 상태 변경이 실제로 커밋되는지 확인한다.
 * RoutineControllerTest 는 서비스를 목으로 대체하기 때문에, PATCH 가 200 을 주면서도
 * DB 에는 반영되지 않던 문제를 잡지 못한다.
 */
class RoutinePersistenceIntegrationTest extends IntegrationTest {

    private static final String WORK_DATE = "2026-09-10";

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
        User user = testUserFixture.createUserWithSettings("routine-persist-" + System.nanoTime());
        authorization = "Bearer " + jwtProvider.createAccessToken(user.getId());
    }

    @Test
    void 루틴_항목을_완료하면_같은_응답의_진행률에_바로_반영된다() throws Exception {
        long taskId = firstTaskId();

        mockMvc.perform(patch("/api/v1/routines/tasks/" + taskId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.progress.done").value(1));
    }

    @Test
    void 루틴_항목을_완료하면_다시_조회해도_완료_상태가_유지된다() throws Exception {
        long taskId = firstTaskId();
        completeTask(taskId);

        mockMvc.perform(get("/api/v1/routines").param("date", WORK_DATE)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks[0].status").value("DONE"))
                .andExpect(jsonPath("$.progress.done").value(1));
    }

    @Test
    void 루틴_항목을_완료하면_DB에_커밋된다() throws Exception {
        long taskId = firstTaskId();
        completeTask(taskId);

        assertThat(routineTaskRepository.findById(taskId).orElseThrow().getStatus())
                .isEqualTo(TaskStatus.DONE);
    }

    /** 코칭을 한 번 조회해야 해당 날짜의 루틴 항목이 생성된다. */
    private long firstTaskId() throws Exception {
        mockMvc.perform(post("/api/v1/shifts")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workDate\":\"%s\",\"shiftType\":\"NIGHT\",\"startTime\":\"22:00\",\"endTime\":\"07:00\"}"
                                .formatted(WORK_DATE)))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/v1/coachings").param("date", WORK_DATE)
                        .header("Authorization", authorization))
                .andExpect(status().isOk());

        String body = mockMvc.perform(get("/api/v1/routines").param("date", WORK_DATE)
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks[0].taskId").exists())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.tasks[0].taskId")).longValue();
    }

    private void completeTask(long taskId) throws Exception {
        mockMvc.perform(patch("/api/v1/routines/tasks/" + taskId)
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());
    }
}
