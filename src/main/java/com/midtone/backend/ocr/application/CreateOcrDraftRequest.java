package com.midtone.backend.ocr.application;

import jakarta.validation.constraints.NotBlank;

/**
 * OCR 이 아예 잡지 못한 날짜를 검수 화면에서 직접 채워 넣을 때 쓰는 요청.
 * 예를 들어 근무 유형이 아닌 배지("교육" 등)가 붙은 칸은 초안이 만들어지지 않는데,
 * 이 경우 수정할 초안 자체가 없어 사용자가 손댈 방법이 없다.
 */
public record CreateOcrDraftRequest(
        @NotBlank(message = "workDate는 필수 입력값입니다.") String workDate,
        @NotBlank(message = "shiftType은 필수 입력값입니다.") String shiftType,
        String startTime,
        String endTime) {
}
