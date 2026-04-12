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
import com.hibol.miette.entity.RecipeAsset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class RecipeMapper {

    @Value("${storage.public-url:}")
    private String storagePublicUrl;

    public RecipeDto toDto(Recipe recipe) {
        return new RecipeDto(
            recipe.getId(),
            recipe.getTitle(),
            recipe.getCreatedAt(),
            recipe.getCreatedBy(),
            recipe.getUpdatedAt(),
            recipe.getUpdatedBy(),
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
                .map(this::toAssetDto)
                .toList()
        );
    }

    public AssetDto toAssetDto(RecipeAsset ra) {
        Asset asset = ra.getAsset();
        String url = null;
        String thumbUrl = null;
        if (asset.getPath() != null && !storagePublicUrl.isBlank()) {
            url = storagePublicUrl + "/" + asset.getPath();
            thumbUrl = storagePublicUrl + "/" + asset.getThumbPath();
        }
        return new AssetDto(
            asset.getId(),
            asset.getDate(),
            asset.getDescription(),
            asset.getType().name(),
            url,
            thumbUrl,
            ra.isCover()
        );
    }

    public AssetDto toAssetDto(Asset asset) {
        String url = null;
        String thumbUrl = null;
        if (asset.getPath() != null && !storagePublicUrl.isBlank()) {
            url = storagePublicUrl + "/" + asset.getPath();
            thumbUrl = storagePublicUrl + "/" + asset.getThumbPath();
        }
        return new AssetDto(
            asset.getId(),
            asset.getDate(),
            asset.getDescription(),
            asset.getType().name(),
            url,
            thumbUrl,
            false
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
            ip.getPosition(),
            ip.getIngredient().getGlutenStrength()
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
