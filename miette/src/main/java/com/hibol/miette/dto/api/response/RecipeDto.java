package com.hibol.miette.dto.api.response;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.time.LocalDateTime;

public record RecipeDto (
    Long id,
    @NotBlank(message = "Le titre est obligatoire") String title,
    LocalDateTime createdAt,
    String createdBy,
    LocalDateTime updatedAt,
    String updatedBy,
    boolean archived,
    List<String> tags,
    @Valid List<PhaseDto> phases,
    List<AssetDto> assets
) {}
