package com.hibol.miette.mapper;

import com.hibol.miette.dto.api.IngredientDto;
import com.hibol.miette.dto.api.PhaseDto;
import com.hibol.miette.dto.api.RecipeDto;
import com.hibol.miette.dto.api.StepDto;
import com.hibol.miette.entity.IngredientPhase;
import com.hibol.miette.entity.Phase;
import com.hibol.miette.entity.Recipe;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class RecipeMapper {

    public RecipeDto toDto(Recipe recipe) {
        return new RecipeDto(
            recipe.getId(),
            recipe.getTitle(),
            recipe.getTags().stream()
                .map(rt -> rt.getTag().getLabel())
                .sorted()
                .toList(),
            recipe.getPhases().stream()
                .sorted(Comparator.comparing(Phase::getPosition))
                .map(this::toPhaseDto)
                .toList()
        );
    }

    private PhaseDto toPhaseDto(Phase phase) {
        return new PhaseDto(
            phase.getId(),
            phase.getLabel(),
            phase.getPosition(),
            phase.getIngredientPhases().stream()
                .sorted(Comparator.comparing(IngredientPhase::getPosition))
                .map(this::toIngredientDto)
                .toList(),
            phase.getSteps().stream()
                .sorted(Comparator.comparing(ip -> ip.getPosition()))
                .map(this::toStepDto)
                .toList()
        );
    }

    private IngredientDto toIngredientDto(IngredientPhase ip) {
        return new IngredientDto(
            ip.getId(),
            ip.getIngredient().getLabel(),
            ip.getQuantity(),
            ip.getIngredient().getUnit(),
            ip.getPosition()
        );
    }

    private StepDto toStepDto(com.hibol.miette.entity.Step step) {
        return new StepDto(
            step.getId(),
            step.getLabel(),
            step.getPosition()
        );
    }
}