package com.hibol.miette.dto.api.response;

public record IngredientDto(
    Long id,
    String label,
    Double quantity,
    String unit,
    Integer position
) {}
