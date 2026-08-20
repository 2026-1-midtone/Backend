package com.midtone.backend.nutrition.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.nutrition.domain.NutrientCode;
import com.midtone.backend.nutrition.domain.NutrientNeedSource;
import com.midtone.backend.nutrition.domain.NutritionProduct;
import com.midtone.backend.nutrition.domain.NutritionProductFunction;
import com.midtone.backend.nutrition.domain.NutritionProductFunctionRepository;
import com.midtone.backend.nutrition.domain.NutritionProductRepository;
import com.midtone.backend.nutrition.domain.UserNutrientNeed;
import com.midtone.backend.nutrition.domain.UserNutrientNeedRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NutritionRecommendationServiceTest {

    @Mock NutritionProductRepository productRepository;
    @Mock NutritionProductFunctionRepository functionRepository;
    @Mock UserNutrientNeedRepository needRepository;
    @Mock CurrentUserIdProvider currentUserIdProvider;

    private NutritionRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new NutritionRecommendationService(
                productRepository, functionRepository, needRepository, currentUserIdProvider);
        given(currentUserIdProvider.getCurrentUserId()).willReturn(1L);
    }

    @Test
    void 등록된_영양소와_일치하는_제품만_일치_개수_순으로_추천한다() {
        given(needRepository.findAllByUserIdOrderByNutrientCodeAsc(1L)).willReturn(List.of(
                new UserNutrientNeed(1L, NutrientCode.MAGNESIUM, NutrientNeedSource.USER_REPORTED,
                        LocalDate.parse("2026-08-18")),
                new UserNutrientNeed(1L, NutrientCode.VITAMIN_D, NutrientNeedSource.HEALTH_CHECK,
                        LocalDate.parse("2026-08-18"))));
        given(productRepository.findAllByOrderByIdAsc()).willReturn(List.of(
                product(1L, "DEEP_SLEEP_VISION", "바이브젠 딥 슬립 앤 비전"),
                product(3L, "REVIVE_ENERGY_SHOT", "바이브젠 리바이브 에너지 샷")));
        given(functionRepository.findAllByProductIdInOrderByProductIdAscSortOrderAsc(List.of(1L, 3L)))
                .willReturn(List.of(
                        function(1L, "마그네슘", NutrientCode.MAGNESIUM),
                        function(3L, "마그네슘", NutrientCode.MAGNESIUM),
                        function(3L, "비타민D", NutrientCode.VITAMIN_D)));

        NutritionRecommendationResponse result = service.getRecommendations();

        assertEquals(2, result.recommendations().size());
        assertEquals("REVIVE_ENERGY_SHOT", result.recommendations().get(0).productCode());
        assertEquals(List.of("MAGNESIUM", "VITAMIN_D"), result.recommendations().get(0).matchedNutrients());
        assertEquals("/images/products/revive_energy_shot.png", result.recommendations().get(0).imageUrl());
        assertEquals("https://www.vivegen.co.kr/energy/?idx=152", result.recommendations().get(0).productUrl());
        assertEquals("DEEP_SLEEP_VISION", result.recommendations().get(1).productCode());
    }

    private NutritionProduct product(long id, String code, String name) {
        return new NutritionProduct(
                id, code, name, code, "/images/products/" + code.toLowerCase() + ".png",
                "https://www.vivegen.co.kr/energy/?idx=152", "건강기능식품은 의약품이 아닙니다.");
    }

    private NutritionProductFunction function(long productId, String ingredient, NutrientCode nutrientCode) {
        return new NutritionProductFunction(productId, ingredient, nutrientCode, "기능정보", 1);
    }
}
