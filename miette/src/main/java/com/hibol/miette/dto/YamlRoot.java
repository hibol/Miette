package com.hibol.miette.dto;

import java.util.List;

import lombok.Data;

@Data
public class YamlRoot {
    private List<YamlRecipe> recipes;
}
