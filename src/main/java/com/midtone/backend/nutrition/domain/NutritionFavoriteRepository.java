package com.midtone.backend.nutrition.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NutritionFavoriteRepository extends JpaRepository<NutritionFavorite, Long> {

    Optional<NutritionFavorite> findByUserIdAndContentId(long userId, long contentId);

    boolean existsByUserIdAndContentId(long userId, long contentId);

    Page<NutritionFavorite> findAllByUserIdOrderByCreatedAtDescIdDesc(long userId, Pageable pageable);

    List<NutritionFavorite> findAllByUserIdAndContentIdIn(long userId, Collection<Long> contentIds);
}
