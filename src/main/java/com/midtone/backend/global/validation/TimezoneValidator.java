package com.midtone.backend.global.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.ZoneId;

public class TimezoneValidator implements ConstraintValidator<Timezone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return ZoneId.getAvailableZoneIds().contains(value);
    }
}
