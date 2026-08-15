package com.midtone.backend.shift.application;

public class ShiftPatternNotFoundException extends RuntimeException {

    public ShiftPatternNotFoundException() {
        super("해당 패턴을 찾을 수 없습니다.");
    }
}
