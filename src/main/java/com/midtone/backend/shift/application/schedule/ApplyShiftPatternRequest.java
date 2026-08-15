package com.midtone.backend.shift.application.schedule;

import com.midtone.backend.global.validation.ValidationPatterns;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ApplyShiftPatternRequest(
        @NotBlank(message = "패턴 적용 시작일은 필수 입력값입니다.")
        @Pattern(regexp = ValidationPatterns.DATE, message = "패턴 적용 시작일은 yyyy-MM-dd 형식이어야 합니다.")
        String startDate,

        @NotNull(message = "생성 기간(주)은 필수 입력값입니다.")
        @Min(value = 4, message = "생성 기간은 최소 4주 이상이어야 합니다.")
        Integer weeks,

        @NotEmpty(message = "패턴은 비어있을 수 없습니다.")
        List<@Pattern(regexp = ValidationPatterns.SHIFT_TYPE, message = "패턴에는 DAY, EVENING, NIGHT, OFF만 포함될 수 있습니다.") String> pattern,

        Boolean saveAsPattern,

        @Size(max = 50, message = "패턴 이름은 50자를 초과할 수 없습니다.")
        String patternName
) {}
