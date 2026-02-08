package com.hibol.miette.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.hibol.miette.dto.RecipeIndexDto;
import com.hibol.miette.entity.Recipe;
import com.hibol.miette.entity.Step;
import com.hibol.miette.repository.RecipeRepository;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RecipeIndexingService {
    
    private final RecipeRepository recipeRepo;
    public RecipeIndexingService(RecipeRepository recipeRepo) {
        this.recipeRepo = recipeRepo;
    }

    public List<RecipeIndexDto> getAllForIndexing() {
        List<Recipe> recipes = recipeRepo.findAll(); // Standard
        return recipes.stream()
            .map(this::buildIndexDto)
            .toList();
    }
    
    private RecipeIndexDto buildIndexDto(Recipe recipe) {
        Set<String> tags = recipe.getTags().stream()
            .map(rt -> rt.getTag().getLabel())
            .collect(Collectors.toSet());
            
        Set<String> ingredients = recipe.getPhases().stream()
            .flatMap(p -> p.getIngredientPhases().stream())
            .map(ip -> ip.getIngredient().getLabel())
            .collect(Collectors.toSet());
            
        Set<String> steps = recipe.getPhases().stream()
            .flatMap(p -> p.getSteps().stream())
            .map(Step::getLabel)
            .collect(Collectors.toSet());
            
        return new RecipeIndexDto(
            recipe.getId(), recipe.getTitle(), 
            tags, ingredients, steps
        );
    }
}

