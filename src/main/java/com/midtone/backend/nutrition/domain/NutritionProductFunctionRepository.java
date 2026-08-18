package com.midtone.backend.nutrition.domain;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutritionProductFunctionRepository extends JpaRepository<NutritionProductFunction, Long> {
    List<NutritionProductFunction> findAllByProductIdInOrderByProductIdAscSortOrderAsc(Collection<Long> productIds);
}
