package com.midtone.backend.global.validation;

public final class ValidationPatterns {

    public static final String DATE = "^\\d{4}-\\d{2}-\\d{2}$";
    public static final String TIME = "^([01]\\d|2[0-3]):[0-5]\\d$";
    public static final String SHIFT_TYPE = "DAY|EVENING|NIGHT|OFF";

    private ValidationPatterns() {
    }
}
