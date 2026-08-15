package com.midtone.backend.shift.application;

public class BulkUpdateRangeException extends RuntimeException {

    public BulkUpdateRangeException() {
        super("변경 기간은 최대 90일까지 지정할 수 있습니다.");
    }
}
