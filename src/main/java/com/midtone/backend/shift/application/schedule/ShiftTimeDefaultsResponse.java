package com.midtone.backend.shift.application.schedule;

import java.util.List;

public record ShiftTimeDefaultsResponse(List<Item> defaults) {

    /**
     * @param source 사용자가 직접 정했으면 USER, 아직 정한 적 없어 대표값을 쓰는 중이면 SYSTEM
     */
    public record Item(String shiftType, String startTime, String endTime, String source) {
    }
}
