package com.midtone.backend.shift.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveShiftPatternRequest(
        @NotBlank(message = "패턴 이름은 필수 입력값입니다.")
        @Size(max = 50, message = "패턴 이름은 50자를 초과할 수 없습니다.")
        String name,
        @NotEmpty(message = "패턴은 필수 입력값입니다.")
        List<@Pattern(regexp = "DAY|EVENING|NIGHT|OFF", message = "패턴에는 DAY, EVENING, NIGHT, OFF만 포함될 수 있습니다.") String> pattern
) {}
