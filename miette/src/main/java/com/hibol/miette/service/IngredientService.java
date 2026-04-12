package com.hibol.miette.service;

import com.hibol.miette.entity.Ingredient;
import com.hibol.miette.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientRepository ingredientRepo;

    public List<Ingredient> findAllSorted() {
        return ingredientRepo.findAllByOrderByLabelAsc();
    }

    public List<Ingredient> findFlourIngredients() {
        return ingredientRepo.findFlourIngredients();
    }

    @Transactional
    public void updateGlutenStrength(Long id, Double value) {
        ingredientRepo.findById(id).ifPresent(ing -> {
            ing.setGlutenStrength(value);
            ingredientRepo.save(ing);
        });
    }

    public List<Ingredient> findOrphans() {
        return ingredientRepo.findOrphans();
    }

    @Transactional
    public int deleteOrphans() {
        List<Ingredient> orphans = ingredientRepo.findOrphans();
        ingredientRepo.deleteAll(orphans);
        return orphans.size();
    }

    /**
     * Retourne l'ingrédient existant portant ce label, ou le crée s'il est inconnu.
     * Si l'ingrédient existe déjà mais avec une unité différente, l'unité est mise à jour.
     */
    @Transactional
    public Ingredient findOrCreate(String label, String unit) {
        Ingredient ingredient = ingredientRepo.findByLabel(label).orElseGet(() -> {
            Ingredient ing = new Ingredient();
            ing.setLabel(label);
            ing.setUnit(unit);
            return ingredientRepo.save(ing);
        });

        if (!Objects.equals(ingredient.getUnit(), unit)) {
            ingredient.setUnit(unit);
            ingredientRepo.save(ingredient);
        }

        return ingredient;
    }
}
