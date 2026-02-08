package com.hibol.miette.dto;

import java.util.Set;

public record RecipeIndexDto(Long id, String title, Set<String> tags, Set<String> ingredients, Set<String> steps) {}
