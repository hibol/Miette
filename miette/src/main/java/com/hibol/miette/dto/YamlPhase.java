package com.hibol.miette.dto;

import java.util.List;

import lombok.Data;

@Data
public class YamlPhase {
    private List<YamlIngredient> ingredients;
    private List<String> steps;
    private String label;
    private Integer position;
}
