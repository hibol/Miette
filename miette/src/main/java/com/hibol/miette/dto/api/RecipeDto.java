package com.hibol.miette.dto.api;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.time.LocalDateTime;

public record RecipeDto (
    Long id,
    @NotBlank String title,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<String> tags,
    List<PhaseDto> phases
) {}