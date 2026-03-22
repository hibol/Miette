package com.hibol.miette.dto.api;

import java.util.List;
import java.time.LocalDateTime;

public record RecipeDto (
    Long id,
    String title,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<String> tags,
    List<PhaseDto> phases
) {}