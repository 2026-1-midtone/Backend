package com.midtone.backend.nutrition.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutritionContentRepository extends JpaRepository<NutritionContent, Long> {

    Page<NutritionContent> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    Page<NutritionContent> findAllByCategoryOrderByCreatedAtDescIdDesc(NutritionCategory category, Pageable pageable);

    Page<NutritionContent> findAllByTimingTagOrderByCreatedAtDescIdDesc(NutritionTimingTag timingTag, Pageable pageable);

    Page<NutritionContent> findAllByContentTypeOrderByCreatedAtDescIdDesc(NutritionContentType contentType, Pageable pageable);

    Page<NutritionContent> findAllByTimingTagAndContentTypeOrderByCreatedAtDescIdDesc(
            NutritionTimingTag timingTag, NutritionContentType contentType, Pageable pageable);
}
