package com.midtone.backend.shift.application;

public class ShiftAccessDeniedException extends RuntimeException {

    public ShiftAccessDeniedException() {
        super("접근 권한이 없습니다.");
    }
}
