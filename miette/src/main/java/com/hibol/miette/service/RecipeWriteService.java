package com.hibol.miette.service;

import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.hibol.miette.dto.api.PhaseDto;
import com.hibol.miette.dto.api.RecipeDto;
import com.hibol.miette.dto.api.StepDto;
import com.hibol.miette.dto.api.IngredientDto;
import com.hibol.miette.entity.Ingredient;
import com.hibol.miette.entity.IngredientPhase;
import com.hibol.miette.entity.Phase;
import com.hibol.miette.entity.Recipe;
import com.hibol.miette.entity.RecipeTag;
import com.hibol.miette.entity.Step;
import com.hibol.miette.entity.Tag;
import com.hibol.miette.repository.IngredientRepository;
import com.hibol.miette.repository.RecipeRepository;
import com.hibol.miette.repository.TagRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeWriteService {

    private final RecipeRepository recipeRepo;
    private final TagRepository tagRepo;
    private final IngredientRepository ingredientRepo;
    private final RecipeIndexingService indexingService;

    @Transactional
    public Recipe update(Long id, RecipeDto dto) {
        Recipe recipe = recipeRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recette introuvable"));

        // Titre
        recipe.setTitle(dto.title());

        // Updated at
        recipe.setUpdatedAt(LocalDateTime.now());

        // Tags — clear et recrée
        recipe.getTags().clear();
        for (String tagLabel : dto.tags()) {
            Tag tag = tagRepo.findByLabel(tagLabel).orElseGet(() -> {
                Tag t = new Tag();
                t.setLabel(tagLabel);
                return tagRepo.save(t);
            });
            RecipeTag recipeTag = new RecipeTag();
            recipeTag.setRecipe(recipe);
            recipeTag.setTag(tag);
            recipe.getTags().add(recipeTag);
        }

        // Phases — clear et recrée
        recipe.getPhases().clear();
        for (int i = 0; i < dto.phases().size(); i++) {
            PhaseDto phaseDto = dto.phases().get(i);
            Phase phase = new Phase();
            phase.setLabel(phaseDto.label());
            phase.setPosition(i + 1);
            phase.setRecipe(recipe);

            // Ingrédients
            for (int j = 0; j < phaseDto.ingredients().size(); j++) {
                IngredientDto ingDto = phaseDto.ingredients().get(j);
                Ingredient ingredient = ingredientRepo.findByLabel(ingDto.label()).orElseGet(() -> {
                    Ingredient ing = new Ingredient();
                    ing.setLabel(ingDto.label());
                    ing.setUnit(ingDto.unit());
                    return ingredientRepo.save(ing);
                });
                IngredientPhase ingPhase = new IngredientPhase();
                ingPhase.setIngredient(ingredient);
                ingPhase.setPhase(phase);
                ingPhase.setQuantity(ingDto.quantity());
                ingPhase.setPosition(j + 1);
                phase.getIngredientPhases().add(ingPhase);
            }

            // Étapes
            for (int j = 0; j < phaseDto.steps().size(); j++) {
                StepDto stepDto = phaseDto.steps().get(j);
                Step step = new Step();
                step.setLabel(stepDto.label());
                step.setPosition(j + 1);
                step.setPhase(phase);
                phase.getSteps().add(step);
            }

            recipe.getPhases().add(phase);
        }

        Recipe saved = recipeRepo.save(recipe);
        indexingService.indexRecipe(saved.getId());
        log.info("✅ Recipe {} updated", saved.getId());
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Recipe recipe = recipeRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recette introuvable"));
        recipeRepo.delete(recipe);
        indexingService.removeFromIndex(id);
        log.info("✅ Recipe {} deleted", id);
    }
}