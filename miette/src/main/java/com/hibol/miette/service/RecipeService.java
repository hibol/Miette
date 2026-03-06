package com.hibol.miette.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.hibol.miette.entity.Recipe;
import com.hibol.miette.entity.RecipeSearchIndex;
import com.hibol.miette.repository.RecipeRepository;
import com.hibol.miette.repository.RecipeSearchIndexRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipeService {

    private final RecipeRepository recipeRepo;
    private final RecipeSearchIndexRepository searchIndexRepo;

    public List<Recipe> findAllWithDetails() {
        return recipeRepo.findAllWithDetails();
    }

    public List<Recipe> search(String query) {
        List<Long> recipeIds = searchIndexRepo.search(query + "*").stream()
            .map(RecipeSearchIndex::getRecipeId)
            .toList();

        if (recipeIds.isEmpty()) return List.of();

        return recipeRepo.findAllWithDetailsByIds(recipeIds);
    }

    public Optional<Recipe> findByIdWithDetails(Long id) {
        return recipeRepo.findByIdWithDetails(id);
    }
}
