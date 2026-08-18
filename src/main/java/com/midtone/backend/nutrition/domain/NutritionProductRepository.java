package com.midtone.backend.nutrition.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutritionProductRepository extends JpaRepository<NutritionProduct, Long> {
    List<NutritionProduct> findAllByOrderByIdAsc();
}
