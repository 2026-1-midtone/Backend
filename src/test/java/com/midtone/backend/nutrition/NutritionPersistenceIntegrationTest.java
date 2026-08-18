package com.midtone.backend.nutrition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.midtone.backend.nutrition.domain.NutrientCode;
import com.midtone.backend.nutrition.domain.NutrientNeedSource;
import com.midtone.backend.nutrition.domain.NutritionProductFunctionRepository;
import com.midtone.backend.nutrition.domain.NutritionProductRepository;
import com.midtone.backend.nutrition.domain.UserNutrientNeed;
import com.midtone.backend.nutrition.domain.UserNutrientNeedRepository;
import com.midtone.backend.support.IntegrationTest;
import com.midtone.backend.support.TestUserFixture;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NutritionPersistenceIntegrationTest extends IntegrationTest {
    @Autowired NutritionProductRepository productRepository;
    @Autowired NutritionProductFunctionRepository functionRepository;
    @Autowired UserNutrientNeedRepository needRepository;
    @Autowired TestUserFixture testUserFixture;

    @Test
    void V10_제품_카탈로그와_사용자_영양소_목표를_저장한다() {
        assertEquals(3, productRepository.count());
        assertEquals(26, functionRepository.count());
        var user = testUserFixture.createUserWithSettings("nutrition-" + System.nanoTime());

        UserNutrientNeed saved = needRepository.save(new UserNutrientNeed(user.getId(), NutrientCode.VITAMIN_D,
                NutrientNeedSource.HEALTH_CHECK, LocalDate.parse("2026-08-18")));

        assertTrue(saved.getId() > 0);
        assertEquals(NutrientCode.VITAMIN_D,
                needRepository.findByUserIdAndNutrientCode(user.getId(), NutrientCode.VITAMIN_D).orElseThrow().getNutrientCode());
    }
}
