package com.midtone.backend.nutrition.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutritionProductRepository extends JpaRepository<NutritionProduct, Long> {
    List<NutritionProduct> findAllByOrderByIdAsc();

    Optional<NutritionProduct> findByCode(String code);
}
