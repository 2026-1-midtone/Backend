package com.midtone.backend.shift;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.midtone.backend.shift.application.ShiftException;
import com.midtone.backend.shift.application.schedule.ApplyShiftPatternRequest;
import com.midtone.backend.shift.application.schedule.ApplyShiftPatternResponse;
import com.midtone.backend.shift.application.schedule.BulkUpdateShiftRequest;
import com.midtone.backend.shift.application.schedule.BulkUpdateShiftResponse;
import com.midtone.backend.shift.application.schedule.CompletenessResponse;
import com.midtone.backend.shift.application.schedule.CreateShiftRequest;
import com.midtone.backend.shift.application.schedule.GetShiftsRequest;
import com.midtone.backend.shift.application.schedule.ShiftCompletenessCalculator;
import com.midtone.backend.shift.application.schedule.ShiftListResponse;
import com.midtone.backend.shift.application.schedule.ShiftPatternApplier;
import com.midtone.backend.shift.application.schedule.ShiftResponse;
import com.midtone.backend.shift.application.schedule.ShiftService;
import com.midtone.backend.shift.application.schedule.UpdateShiftRequest;
import com.midtone.backend.shift.application.schedule.UpdateShiftResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ShiftController.class)
@AutoConfigureMockMvc(addFilters = false)
class ShiftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShiftService shiftService;
    @MockitoBean
    private ShiftPatternApplier shiftPatternApplier;
    @MockitoBean
    private ShiftCompletenessCalculator shiftCompletenessCalculator;

    @Test
    void createsShiftAndReturnsCreated() throws Exception {
        given(shiftService.createShift(any(CreateShiftRequest.class))).willReturn(
                new ShiftResponse(1L, "2026-08-29", "NIGHT", "22:00", "07:00", "MANUAL", true));

        mockMvc.perform(post("/api/v1/shifts")
                        .contentType("application/json")
                        .content("{\"workDate\":\"2026-08-29\",\"shiftType\":\"NIGHT\",\"startTime\":\"22:00\",\"endTime\":\"07:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shiftId").value(1))
                .andExpect(jsonPath("$.shiftType").value("NIGHT"));
    }

    @Test
    void rejectsDuplicateShiftCreation() throws Exception {
        given(shiftService.createShift(any(CreateShiftRequest.class)))
                .willThrow(new ShiftException(ShiftException.ErrorCode.DUPLICATE_SHIFT));

        mockMvc.perform(post("/api/v1/shifts")
                        .contentType("application/json")
                        .content("{\"workDate\":\"2026-08-29\",\"shiftType\":\"NIGHT\",\"startTime\":\"22:00\",\"endTime\":\"07:00\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("해당 날짜에 이미 근무 일정이 있습니다."));
    }

    @Test
    void returnsShiftsInRange() throws Exception {
        given(shiftService.getShifts(any(GetShiftsRequest.class))).willReturn(
                new ShiftListResponse(List.of()));

        mockMvc.perform(get("/api/v1/shifts").param("from", "2026-08-01").param("to", "2026-08-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shifts").isArray());
    }

    @Test
    void updatesShiftAndReturnsAffectedCoachingDates() throws Exception {
        given(shiftService.updateShift(org.mockito.ArgumentMatchers.eq(505L), any(UpdateShiftRequest.class)))
                .willReturn(new UpdateShiftResponse(505L, "2026-08-05", "EVENING", "14:00", "22:00", true, List.of("2026-08-05")));

        mockMvc.perform(patch("/api/v1/shifts/505")
                        .contentType("application/json")
                        .content("{\"shiftType\":\"EVENING\",\"startTime\":\"14:00\",\"endTime\":\"22:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shiftType").value("EVENING"))
                .andExpect(jsonPath("$.affectedCoachingDates[0]").value("2026-08-05"));
    }

    @Test
    void deletesShiftAndReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/shifts/505"))
                .andExpect(status().isNoContent());
    }

    @Test
    void bulkUpdatesShiftsInRange() throws Exception {
        given(shiftService.bulkUpdateShifts(any(BulkUpdateShiftRequest.class)))
                .willReturn(new BulkUpdateShiftResponse(5, List.of("2026-08-10")));

        mockMvc.perform(patch("/api/v1/shifts:bulk")
                        .contentType("application/json")
                        .content("{\"from\":\"2026-08-10\",\"to\":\"2026-08-14\",\"shiftType\":\"NIGHT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(5));
    }

    @Test
    void appliesShiftPatternAndReturnsCreated() throws Exception {
        given(shiftPatternApplier.apply(any(ApplyShiftPatternRequest.class))).willReturn(
                ApplyShiftPatternResponse.of(28, 0, null, new CompletenessResponse(28, 28, 0, List.of())));

        mockMvc.perform(post("/api/v1/shifts/pattern")
                        .contentType("application/json")
                        .content("{\"startDate\":\"2026-09-01\",\"weeks\":4,\"pattern\":[\"DAY\",\"EVENING\",\"NIGHT\",\"OFF\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdCount").value(28));
    }

    @Test
    void returnsCompletenessForDefaultFourWeeks() throws Exception {
        given(shiftCompletenessCalculator.calculate(4)).willReturn(new CompletenessResponse(28, 28, 0, List.of()));

        mockMvc.perform(get("/api/v1/shifts/completeness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredDays").value(28))
                .andExpect(jsonPath("$.remainingDays").value(0));
    }
}
