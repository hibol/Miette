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
import com.hibol.miette.entity.IngredientPhase;
import com.hibol.miette.entity.Phase;
import com.hibol.miette.entity.Recipe;
import com.hibol.miette.entity.RecipeTag;
import com.hibol.miette.entity.Step;
import com.hibol.miette.entity.Tag;
import com.hibol.miette.repository.RecipeRepository;
import com.hibol.miette.repository.TagRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeWriteService {

    private final RecipeRepository recipeRepo;
    private final TagRepository tagRepo;
    private final IngredientService ingredientService;
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
            recipe.getPhases().add(buildPhase(dto.phases().get(i), i + 1, recipe));
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

    @Transactional
    public Recipe create(RecipeDto dto) {
        Recipe recipe = new Recipe();
        recipe.setTitle(dto.title());
        recipe.setCreatedAt(LocalDateTime.now());

        // Tags
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

        // Phases
        for (int i = 0; i < dto.phases().size(); i++) {
            recipe.getPhases().add(buildPhase(dto.phases().get(i), i + 1, recipe));
        }

        Recipe saved = recipeRepo.save(recipe);
        indexingService.indexRecipe(saved.getId());
        log.info("✅ Recipe {} created", saved.getId());
        return saved;
    }

    /**
     * Construit une Phase à partir d'un PhaseDto.
     * - Les ingrédients avec un label vide sont ignorés (évite de polluer la table ingredient).
     * - Si un ingrédient existe déjà et que son unité a changé, l'unité est mise à jour.
     * - Les étapes avec un label vide sont ignorées.
     */
    private Phase buildPhase(PhaseDto dto, int position, Recipe recipe) {
        Phase phase = new Phase();
        phase.setLabel(dto.label());
        phase.setPosition(position);
        phase.setRecipe(recipe);

        int ingPos = 0;
        for (IngredientDto ingDto : dto.ingredients()) {
            if (ingDto.label() == null || ingDto.label().isBlank()) continue;
            ingPos++;

            IngredientPhase ingPhase = new IngredientPhase();
            ingPhase.setIngredient(ingredientService.findOrCreate(ingDto.label(), ingDto.unit()));
            ingPhase.setPhase(phase);
            ingPhase.setQuantity(ingDto.quantity());
            ingPhase.setPosition(ingPos);
            phase.getIngredientPhases().add(ingPhase);
        }

        int stepPos = 0;
        for (StepDto stepDto : dto.steps()) {
            if (stepDto.label() == null || stepDto.label().isBlank()) continue;
            stepPos++;

            Step step = new Step();
            step.setLabel(stepDto.label());
            step.setPosition(stepPos);
            step.setPhase(phase);
            phase.getSteps().add(step);
        }

        return phase;
    }
}