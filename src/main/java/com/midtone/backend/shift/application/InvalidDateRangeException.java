package com.midtone.backend.shift.application;

public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException() {
        super("조회 종료일은 시작일보다 빨라야 합니다.");
    }
}
