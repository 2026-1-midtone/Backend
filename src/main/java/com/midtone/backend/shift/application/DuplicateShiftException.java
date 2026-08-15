package com.midtone.backend.shift.application;

public class DuplicateShiftException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "해당 날짜에 이미 근무 일정이 있습니다.";

    public DuplicateShiftException() {
        super(DEFAULT_MESSAGE);
    }
}
