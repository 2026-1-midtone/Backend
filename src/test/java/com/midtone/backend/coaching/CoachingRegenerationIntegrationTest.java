package com.midtone.backend.coaching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.midtone.backend.coaching.application.CoachingService;
import com.midtone.backend.coaching.application.RegenerateCoachingRequest;
import com.midtone.backend.coaching.application.RegenerateCoachingResponse;
import com.midtone.backend.coaching.domain.CoachingCardRepository;
import com.midtone.backend.coaching.domain.DailyCoaching;
import com.midtone.backend.coaching.domain.DailyCoachingRepository;
import com.midtone.backend.shift.domain.ShiftSchedule;
import com.midtone.backend.shift.domain.ShiftScheduleRepository;
import com.midtone.backend.shift.domain.ShiftTime;
import com.midtone.backend.shift.domain.ShiftType;
import com.midtone.backend.support.IntegrationTest;
import com.midtone.backend.support.TestUserFixture;
import com.midtone.backend.user.domain.User;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class CoachingRegenerationIntegrationTest extends IntegrationTest {

    private static final LocalDate COACHING_DATE = LocalDate.of(2026, 8, 10);

    @Autowired
    private CoachingService coachingService;

    @Autowired
    private ShiftScheduleRepository shiftScheduleRepository;

    @Autowired
    private DailyCoachingRepository dailyCoachingRepository;

    @Autowired
    private CoachingCardRepository coachingCardRepository;

    @Autowired
    private TestUserFixture testUserFixture;

    private User user;

    @BeforeEach
    void setUp() {
        dailyCoachingRepository.deleteAll();
        shiftScheduleRepository.deleteAll();
        user = testUserFixture.createUserWithSettings("coaching-regen-" + System.nanoTime());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getId(), null, List.of()));
        shiftScheduleRepository.save(new ShiftSchedule(
                user.getId(), COACHING_DATE, ShiftType.NIGHT,
                new ShiftTime(LocalTime.of(22, 0), LocalTime.of(7, 0))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 이미_코칭이_있는_날짜를_재생성해도_유니크_제약에_걸리지_않는다() {
        coachingService.getTodayCoaching(COACHING_DATE);
        DailyCoaching before = dailyCoachingRepository
                .findByUserIdAndCoachingDate(user.getId(), COACHING_DATE)
                .orElseThrow();

        RegenerateCoachingResponse response = coachingService.regenerateCoaching(
                new RegenerateCoachingRequest(COACHING_DATE.toString(), COACHING_DATE.toString()));

        assertEquals(1, response.regeneratedCount());
        DailyCoaching after = dailyCoachingRepository
                .findByUserIdAndCoachingDate(user.getId(), COACHING_DATE)
                .orElseThrow();
        assertNotEquals(before.getId(), after.getId());
        assertEquals(1, dailyCoachingRepository.count());
    }

    @Test
    void 재생성을_반복해도_코칭_카드가_중복_누적되지_않는다() {
        coachingService.getTodayCoaching(COACHING_DATE);
        long cardCountAfterFirstGeneration = coachingCardRepository.count();

        coachingService.regenerateCoaching(
                new RegenerateCoachingRequest(COACHING_DATE.toString(), COACHING_DATE.toString()));
        coachingService.regenerateCoaching(
                new RegenerateCoachingRequest(COACHING_DATE.toString(), COACHING_DATE.toString()));

        assertTrue(cardCountAfterFirstGeneration > 0);
        assertEquals(cardCountAfterFirstGeneration, coachingCardRepository.count());
    }
}
