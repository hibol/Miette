package com.hibol.miette.mapper;

import com.hibol.miette.dto.api.response.AssetDto;
import com.hibol.miette.dto.api.response.IngredientDto;
import com.hibol.miette.dto.api.response.PhaseDto;
import com.hibol.miette.dto.api.response.RecipeDto;
import com.hibol.miette.dto.api.response.StepDto;
import com.hibol.miette.entity.Asset;
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
            recipe.getCreatedAt(),
            recipe.getUpdatedAt(),
            recipe.getTags().stream()
                .map(rt -> rt.getTag().getLabel())
                .sorted()
                .toList(),
            recipe.getPhases().stream()
                .sorted(Comparator.comparing(Phase::getPosition))
                .map(this::toPhaseDto)
                .toList(),
            recipe.getAssets().stream()
                .sorted(Comparator.comparing(ra -> ra.getAsset().getDate(), Comparator.reverseOrder()))
                .map(ra -> toAssetDto(ra.getAsset()))
                .toList()
        );
    }

    public AssetDto toAssetDto(Asset asset) {
        return new AssetDto(
            asset.getId(),
            asset.getDate(),
            asset.getDescription(),
            asset.getType().name()
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
