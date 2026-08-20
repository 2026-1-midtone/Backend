package com.midtone.backend.global.config;

import com.midtone.backend.auth.application.AuthService;
import com.midtone.backend.auth.domain.LogoutRepository;
import com.midtone.backend.auth.jwt.JwtProvider;
import com.midtone.backend.caffeine.application.CaffeineIntakeService;
import com.midtone.backend.chat.application.ChatService;
import com.midtone.backend.coaching.application.CoachingService;
import com.midtone.backend.home.application.HomeService;
import com.midtone.backend.nap.application.NapService;
import com.midtone.backend.nutrition.application.NutrientNeedService;
import com.midtone.backend.nutrition.application.NutritionContentService;
import com.midtone.backend.nutrition.application.NutritionRecommendationService;
import com.midtone.backend.ocr.application.OcrJobService;
import com.midtone.backend.routine.application.RoutineService;
import com.midtone.backend.shift.application.pattern.ShiftPatternService;
import com.midtone.backend.shift.application.schedule.ShiftCompletenessCalculator;
import com.midtone.backend.shift.application.schedule.ShiftPatternApplier;
import com.midtone.backend.shift.application.schedule.ShiftService;
import com.midtone.backend.shift.application.schedule.ShiftTimeDefaultService;
import com.midtone.backend.sleep.application.SleepLogService;
import com.midtone.backend.transition.application.TransitionService;
import com.midtone.backend.user.application.notification.NotificationSettingService;
import com.midtone.backend.user.application.profile.UserService;
import com.midtone.backend.user.application.settings.UserSettingsService;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
@ActiveProfiles("test")
class SecurityConfigTest {

    private static final String UNMAPPED_PROTECTED_PATH = "/api/v1/__does-not-exist__";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NapService napService;

    @MockitoBean
    private RoutineService routineService;

    @MockitoBean
    private OcrJobService ocrJobService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private LogoutRepository logoutRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserSettingsService userSettingsService;

    @MockitoBean
    private NotificationSettingService notificationSettingService;

    @MockitoBean
    private ShiftService shiftService;

    @MockitoBean
    private ShiftPatternApplier shiftPatternApplier;

    @MockitoBean
    private ShiftTimeDefaultService shiftTimeDefaultService;

    @MockitoBean
    private ShiftCompletenessCalculator shiftCompletenessCalculator;

    @MockitoBean
    private ShiftPatternService shiftPatternService;

    @MockitoBean
    private CoachingService coachingService;

    @MockitoBean
    private TransitionService transitionService;

    @MockitoBean
    private HomeService homeService;

    @MockitoBean
    private SleepLogService sleepLogService;

    @MockitoBean
    private CaffeineIntakeService caffeineIntakeService;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private Clock clock;

    @MockitoBean
    private NutritionContentService nutritionContentService;

    @MockitoBean
    private NutrientNeedService nutrientNeedService;

    @MockitoBean
    private NutritionRecommendationService nutritionRecommendationService;

    @Test
    void unauthenticatedApiRequestReturnsJsonUnauthorizedResponse() throws Exception {
        mockMvc.perform(get(UNMAPPED_PROTECTED_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    @Test
    void validAccessTokenPassesAuthenticationAndReachesDispatcher() throws Exception {
        when(jwtProvider.isValid("valid-token")).thenReturn(true);
        when(jwtProvider.isAccessToken("valid-token")).thenReturn(true);
        when(jwtProvider.getUserId("valid-token")).thenReturn(1L);
        when(logoutRepository.findByUserId(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get(UNMAPPED_PROTECTED_PATH).header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound());
    }
}
