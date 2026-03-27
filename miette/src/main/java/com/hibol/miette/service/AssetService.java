package com.hibol.miette.service;

import com.hibol.miette.dto.api.request.NoteCreateDto;
import com.hibol.miette.entity.Asset;
import com.hibol.miette.entity.Recipe;
import com.hibol.miette.entity.RecipeAsset;
import com.hibol.miette.repository.AssetRepository;
import com.hibol.miette.repository.RecipeAssetRepository;
import com.hibol.miette.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepo;
    private final RecipeAssetRepository recipeAssetRepo;
    private final RecipeRepository recipeRepo;

    @Transactional
    public Asset addNote(Long recipeId, NoteCreateDto dto) {
        Recipe recipe = recipeRepo.findById(recipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recette introuvable"));

        Asset asset = new Asset();
        asset.setDate(dto.date());
        asset.setDescription(dto.description());
        assetRepo.save(asset);

        RecipeAsset link = new RecipeAsset();
        link.setRecipe(recipe);
        link.setAsset(asset);
        recipeAssetRepo.save(link);

        return asset;
    }

    @Transactional
    public void deleteAsset(Long recipeId, Long assetId) {
        RecipeAsset link = recipeAssetRepo.findByRecipeIdAndAssetId(recipeId, assetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset introuvable"));
        recipeAssetRepo.delete(link);
        assetRepo.deleteById(assetId);
    }
}
