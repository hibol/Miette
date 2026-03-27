package com.hibol.miette.dto.api.response;

import java.util.List;

public record PhaseDto(
    Long id,
    String label,
    Integer position,
    List<IngredientDto> ingredients,
    List<StepDto> steps
) {}
