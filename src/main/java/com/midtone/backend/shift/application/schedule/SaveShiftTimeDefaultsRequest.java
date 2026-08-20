package com.midtone.backend.shift.application.schedule;

import com.midtone.backend.global.validation.ValidationPatterns;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 근무 유형별 기본 시각 저장 요청. 보낸 유형만 저장하고, 빠뜨린 유형은 시스템 기본값으로 남는다.
 */
public record SaveShiftTimeDefaultsRequest(
        @NotNull(message = "defaults는 필수 입력값입니다.")
        @Size(max = 3, message = "기본 시각은 최대 3개까지 지정할 수 있습니다.")
        List<@Valid Item> defaults) {

    public record Item(
            @NotBlank(message = "근무 유형은 필수 입력값입니다.")
            @Pattern(regexp = ValidationPatterns.SHIFT_TYPE,
                    message = "근무 유형은 DAY, EVENING, NIGHT, OFF 중 하나여야 합니다.")
            String shiftType,

            @NotBlank(message = "근무 시작 시각은 필수 입력값입니다.")
            @Pattern(regexp = ValidationPatterns.TIME, message = "근무 시작 시각은 HH:mm 형식이어야 합니다.")
            String startTime,

            @NotBlank(message = "근무 종료 시각은 필수 입력값입니다.")
            @Pattern(regexp = ValidationPatterns.TIME, message = "근무 종료 시각은 HH:mm 형식이어야 합니다.")
            String endTime) {
    }
}
