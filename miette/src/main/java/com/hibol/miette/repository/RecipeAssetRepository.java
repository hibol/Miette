package com.hibol.miette.repository;

import com.hibol.miette.entity.RecipeAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecipeAssetRepository extends JpaRepository<RecipeAsset, Long> {
    Optional<RecipeAsset> findByRecipeIdAndAssetId(Long recipeId, Long assetId);
    java.util.List<RecipeAsset> findByRecipeId(Long recipeId);
}
