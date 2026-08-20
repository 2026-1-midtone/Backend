package com.midtone.backend.caffeine.application;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 하루 300mg 이상의 카페인 섭취는 불안·불쾌감을 유발하고 수면시간·수면효율에 영향을 준다는 국내 연구
 * (Lee, Yoo, Lee, Park, & Kim, 2007 — 김혜성·이종은, 2020에서 재인용)에 근거해 실제 섭취 총량(mg)
 * 기준으로 초과 여부를 판단한다. "1잔" 단위는 음료 종류·양에 따라 실제 mg 편차가 커서(예: 25~67mg/150mL)
 * 판단 기준으로 쓰지 않는다.
 */
public record DailyCaffeineStatus(
        LocalDate date,
        int totalAmountMg,
        BigDecimal totalServings,
        boolean overDailyLimit) {
}
