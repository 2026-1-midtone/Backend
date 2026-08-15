package com.midtone.backend.shift.application;

public class ShiftNotFoundException extends RuntimeException {

    public ShiftNotFoundException() {
        super("해당 근무 일정이 없습니다.");
    }
}
