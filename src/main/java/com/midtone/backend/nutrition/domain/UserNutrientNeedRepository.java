package com.midtone.backend.nutrition.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserNutrientNeedRepository extends JpaRepository<UserNutrientNeed, Long> {
    List<UserNutrientNeed> findAllByUserIdOrderByNutrientCodeAsc(long userId);
    Optional<UserNutrientNeed> findByUserIdAndNutrientCode(long userId, NutrientCode nutrientCode);
    void deleteAllByUserId(long userId);
}
