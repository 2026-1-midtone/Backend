package com.midtone.backend.shift;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.shift.application.ShiftException;
import com.midtone.backend.shift.application.pattern.SaveShiftPatternRequest;
import com.midtone.backend.shift.application.pattern.ShiftPatternListResponse;
import com.midtone.backend.shift.application.pattern.ShiftPatternResponse;
import com.midtone.backend.shift.application.pattern.ShiftPatternService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShiftPatternController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShiftPatternControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShiftPatternService shiftPatternService;

    @Test
    void returnsSavedShiftPatterns() throws Exception {
        given(shiftPatternService.getShiftPatterns()).willReturn(new ShiftPatternListResponse(
                List.of(new ShiftPatternResponse(1L, "내 패턴", 4, List.of("DAY", "EVENING", "NIGHT", "OFF")))));

        mockMvc.perform(get("/api/v1/shift-patterns"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patterns[0].name").value("내 패턴"))
                .andExpect(jsonPath("$.patterns[0].cycleDays").value(4));
    }

    @Test
    void savesShiftPatternAndReturnsCreated() throws Exception {
        given(shiftPatternService.saveShiftPattern(any(SaveShiftPatternRequest.class))).willReturn(
                new ShiftPatternResponse(2L, "새 패턴", 3, List.of("DAY", "NIGHT", "OFF")));

        mockMvc.perform(post("/api/v1/shift-patterns")
                        .contentType("application/json")
                        .content("{\"name\":\"새 패턴\",\"pattern\":[\"DAY\",\"NIGHT\",\"OFF\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patternId").value(2))
                .andExpect(jsonPath("$.name").value("새 패턴"));
    }

    @Test
    void rejectsSaveWithoutName() throws Exception {
        mockMvc.perform(post("/api/v1/shift-patterns")
                        .contentType("application/json")
                        .content("{\"pattern\":[\"DAY\",\"NIGHT\",\"OFF\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("패턴 이름은 필수 입력값입니다."));
    }

    @Test
    void deletesShiftPatternAndReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/shift-patterns/2"))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsDeleteWhenPatternNotFound() throws Exception {
        org.mockito.Mockito.doThrow(new ShiftException(ShiftException.ErrorCode.SHIFT_PATTERN_NOT_FOUND))
                .when(shiftPatternService).deleteShiftPattern(99L);

        mockMvc.perform(delete("/api/v1/shift-patterns/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("해당 패턴을 찾을 수 없습니다."));
    }
}
