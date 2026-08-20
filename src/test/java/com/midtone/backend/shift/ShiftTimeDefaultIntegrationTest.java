package com.midtone.backend.shift;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.auth.jwt.JwtProvider;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.UserShiftTimeDefaultRepository;
import com.midtone.backend.support.IntegrationTest;
import com.midtone.backend.support.TestUserFixture;
import com.midtone.backend.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 근무 유형별 기본 시각을 사용자가 직접 정할 수 있는지 확인한다.
 * 시각이 비어 있으면 코칭 카드·다음 근무·야간 영양 타이밍이 모두 계산되지 않으므로,
 * 시각을 생략하고 근무를 만들면 사용자가 정한 기본값이 채워져야 한다.
 */
class ShiftTimeDefaultIntegrationTest extends IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private ShiftScheduleRepository shiftScheduleRepository;

    @Autowired
    private UserShiftTimeDefaultRepository userShiftTimeDefaultRepository;

    @Autowired
    private TestUserFixture testUserFixture;

    private String authorization;

    @BeforeEach
    void setUp() {
        shiftScheduleRepository.deleteAll();
        userShiftTimeDefaultRepository.deleteAll();
        User user = testUserFixture.createUserWithSettings("shift-time-default-" + System.nanoTime());
        authorization = "Bearer " + jwtProvider.createAccessToken(user.getId());
    }

    @Test
    void 설정한_적이_없으면_시스템_기본값을_내려준다() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/shift-time-defaults").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaults.length()").value(3))
                .andExpect(jsonPath("$.defaults[0].shiftType").value("DAY"))
                .andExpect(jsonPath("$.defaults[0].source").value("SYSTEM"))
                .andExpect(jsonPath("$.defaults[2].shiftType").value("NIGHT"))
                .andExpect(jsonPath("$.defaults[2].startTime").value("22:00"));
    }

    @Test
    void 저장한_기본_시각을_다시_조회할_수_있다() throws Exception {
        saveDefaults();

        mockMvc.perform(get("/api/v1/users/me/shift-time-defaults").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaults[0].shiftType").value("DAY"))
                .andExpect(jsonPath("$.defaults[0].startTime").value("07:00"))
                .andExpect(jsonPath("$.defaults[0].endTime").value("15:00"))
                .andExpect(jsonPath("$.defaults[0].source").value("USER"));
    }

    @Test
    void 일부만_저장하면_나머지는_시스템_기본값으로_남는다() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/shift-time-defaults")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"defaults\":[{\"shiftType\":\"NIGHT\",\"startTime\":\"23:00\",\"endTime\":\"07:00\"}]}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/me/shift-time-defaults").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaults[0].source").value("SYSTEM"))
                .andExpect(jsonPath("$.defaults[2].startTime").value("23:00"))
                .andExpect(jsonPath("$.defaults[2].source").value("USER"));
    }

    @Test
    void 시각을_생략하고_근무를_만들면_사용자_기본값이_채워진다() throws Exception {
        saveDefaults();

        mockMvc.perform(post("/api/v1/shifts")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workDate\":\"2026-09-10\",\"shiftType\":\"NIGHT\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startTime").value("23:00"))
                .andExpect(jsonPath("$.endTime").value("07:30"));
    }

    @Test
    void 시각을_직접_주면_기본값보다_우선한다() throws Exception {
        saveDefaults();

        mockMvc.perform(post("/api/v1/shifts")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workDate\":\"2026-09-11\",\"shiftType\":\"NIGHT\","
                                + "\"startTime\":\"21:00\",\"endTime\":\"06:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startTime").value("21:00"));
    }

    @Test
    void OFF는_기본_시각을_설정할_수_없다() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/shift-time-defaults")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"defaults\":[{\"shiftType\":\"OFF\",\"startTime\":\"09:00\",\"endTime\":\"18:00\"}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void OFF_근무에는_기본_시각을_채우지_않는다() throws Exception {
        saveDefaults();

        mockMvc.perform(post("/api/v1/shifts")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workDate\":\"2026-09-12\",\"shiftType\":\"OFF\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.startTime").doesNotExist())
                .andExpect(jsonPath("$.endTime").doesNotExist());
    }

    @Test
    void 패턴으로_만든_근무에도_사용자_기본값이_채워진다() throws Exception {
        saveDefaults();

        mockMvc.perform(post("/api/v1/shifts/pattern")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2026-09-14\",\"weeks\":4,"
                                + "\"pattern\":[\"DAY\",\"NIGHT\",\"OFF\"]}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/shifts")
                        .header("Authorization", authorization)
                        .param("from", "2026-09-14")
                        .param("to", "2026-09-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shifts[0].shiftType").value("DAY"))
                .andExpect(jsonPath("$.shifts[0].startTime").value("07:00"))
                .andExpect(jsonPath("$.shifts[0].endTime").value("15:00"))
                .andExpect(jsonPath("$.shifts[1].shiftType").value("NIGHT"))
                .andExpect(jsonPath("$.shifts[1].startTime").value("23:00"))
                .andExpect(jsonPath("$.shifts[1].endTime").value("07:30"))
                .andExpect(jsonPath("$.shifts[2].shiftType").value("OFF"))
                .andExpect(jsonPath("$.shifts[2].startTime").doesNotExist());
    }

    @Test
    void 패턴으로_근무_유형이_바뀌면_시각도_새_유형의_기본값으로_바뀐다() throws Exception {
        saveDefaults();
        mockMvc.perform(post("/api/v1/shifts")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workDate\":\"2026-09-14\",\"shiftType\":\"DAY\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/shifts/pattern")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2026-09-14\",\"weeks\":4,\"pattern\":[\"NIGHT\"]}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/shifts")
                        .header("Authorization", authorization)
                        .param("from", "2026-09-14")
                        .param("to", "2026-09-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shifts[0].shiftType").value("NIGHT"))
                .andExpect(jsonPath("$.shifts[0].startTime").value("23:00"))
                .andExpect(jsonPath("$.shifts[0].endTime").value("07:30"));
    }

    private void saveDefaults() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/shift-time-defaults")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"defaults":[
                                  {"shiftType":"DAY","startTime":"07:00","endTime":"15:00"},
                                  {"shiftType":"EVENING","startTime":"15:00","endTime":"23:00"},
                                  {"shiftType":"NIGHT","startTime":"23:00","endTime":"07:30"}
                                ]}"""))
                .andExpect(status().isOk());
    }
}
