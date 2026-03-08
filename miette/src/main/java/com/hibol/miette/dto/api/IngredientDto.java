package com.hibol.miette.dto.api;

public record IngredientDto(
    Long id,
    String label,
    Double quantity,
    String unit,
    Integer position
) {}
