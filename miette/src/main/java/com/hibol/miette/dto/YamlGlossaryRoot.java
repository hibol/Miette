package com.hibol.miette.dto;

import lombok.Data;

import java.util.List;

@Data
public class YamlGlossaryRoot {
    private List<YamlGlossaryEntry> glossary;
}
