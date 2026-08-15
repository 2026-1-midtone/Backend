package com.midtone.backend.shift.application;

public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException() {
        super("to는 from보다 빠를 수 없습니다.");
    }
}
