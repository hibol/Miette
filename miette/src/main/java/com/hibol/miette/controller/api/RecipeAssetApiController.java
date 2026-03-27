package com.hibol.miette.controller.api;

import com.hibol.miette.dto.api.response.AssetDto;
import com.hibol.miette.dto.api.request.NoteCreateDto;
import com.hibol.miette.entity.Asset;
import com.hibol.miette.mapper.RecipeMapper;
import com.hibol.miette.service.AssetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recipes/{recipeId}/assets")
@RequiredArgsConstructor
public class RecipeAssetApiController {

    private final AssetService assetService;
    private final RecipeMapper recipeMapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssetDto> addNote(@PathVariable Long recipeId, @Valid @RequestBody NoteCreateDto dto) {
        Asset asset = assetService.addNote(recipeId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(recipeMapper.toAssetDto(asset));
    }

    @DeleteMapping("/{assetId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long recipeId, @PathVariable Long assetId) {
        assetService.deleteAsset(recipeId, assetId);
        return ResponseEntity.noContent().build();
    }
}
