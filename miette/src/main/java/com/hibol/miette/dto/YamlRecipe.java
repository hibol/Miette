package com.hibol.miette.dto;

import java.util.List;

import lombok.Data;

@Data
public class YamlRecipe {
    private String title;
    private List<String> tags;
    private List<YamlIngredient> ingredients;
    private List<String> steps;
    private List<YamlPhase> phases;
}
