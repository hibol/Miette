package com.hibol.miette.dto.api;

import java.util.List;

public record RecipeDto (
    Long id,
    String title,
    List<String> tags,
    List<PhaseDto> phases
) {}