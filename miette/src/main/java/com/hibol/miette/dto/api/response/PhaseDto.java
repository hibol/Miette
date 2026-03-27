package com.hibol.miette.dto.api.response;

import jakarta.validation.Valid;
import java.util.List;

public record PhaseDto(
    Long id,
    String label,
    Integer position,
    @Valid List<IngredientDto> ingredients,
    List<StepDto> steps
) {}
