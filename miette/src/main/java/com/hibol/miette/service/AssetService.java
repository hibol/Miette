package com.hibol.miette.service;

import com.hibol.miette.dto.api.request.NoteCreateDto;
import com.hibol.miette.entity.Asset;
import com.hibol.miette.entity.Recipe;
import com.hibol.miette.entity.RecipeAsset;
import com.hibol.miette.repository.AssetRepository;
import com.hibol.miette.repository.RecipeAssetRepository;
import com.hibol.miette.repository.RecipeRepository;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepo;
    private final RecipeAssetRepository recipeAssetRepo;
    private final RecipeRepository recipeRepo;

    @Autowired(required = false)
    private StorageService storageService;

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
    public Asset uploadPhoto(Long recipeId, MultipartFile file) throws IOException {
        if (storageService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Storage non configuré");
        }
        Recipe recipe = recipeRepo.findById(recipeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recette introuvable"));

        String ext = getExtension(file.getOriginalFilename());
        String key = "recipe_" + recipeId + "_" + UUID.randomUUID() + "." + ext;

        LocalDateTime photoDate = extractExifDate(file);
        storageService.upload(file, key);

        Asset asset = new Asset();
        asset.setDate(photoDate != null ? photoDate : LocalDateTime.now());
        asset.setPath(key);
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
        String path = link.getAsset().getPath();
        recipeAssetRepo.delete(link);
        assetRepo.deleteById(assetId);
        if (path != null && storageService != null) {
            try {
                storageService.delete(path);
            } catch (Exception e) {
                log.warn("⚠️ Échec suppression S3 '{}' : {}", path, e.getMessage());
            }
        }
    }

    private LocalDateTime extractExifDate(MultipartFile file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file.getInputStream());
            ExifSubIFDDirectory dir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (dir != null) {
                Date date = dir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getExtension(String filename) {
        if (filename == null) return "jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "jpg";
    }
}
