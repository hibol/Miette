package com.hibol.miette.service;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.hibol.miette.entity.Asset;
import com.hibol.miette.entity.Recipe;
import com.hibol.miette.repository.RecipeRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipeService {

    private final RecipeRepository recipeRepo;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Recipe> findAllWithDetails() {
        return recipeRepo.findAllWithDetails();
    }

    public List<Recipe> search(String query) {
        return Search.session(entityManager)
            .search(Recipe.class)
            .where(f -> f.simpleQueryString()
                .fields("title",
                        "tags.tag.label",
                        "phases.steps.label",
                        "phases.ingredientPhases.ingredient.label")
                .matching(query))
            .fetchHits(500);
    }

    public Optional<Recipe> findByIdWithDetails(Long id) {
        return recipeRepo.findByIdWithDetails(id);
    }

    public List<Recipe> filterByAssetType(List<Recipe> recipes, Asset.AssetType type) {
        return recipes.stream()
                .filter(r -> r.getAssets().stream().anyMatch(a -> a.getAsset().getType() == type))
                .toList();
    }
}
