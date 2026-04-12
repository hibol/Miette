package com.hibol.miette.dto.api.response;

import jakarta.validation.constraints.Positive;

public record IngredientDto(
    Long id,
    String label,
    @Positive(message = "La quantité doit être positive") Double quantity,
    String unit,
    Integer position,
    Double glutenStrength
) {}
