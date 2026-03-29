package com.hibol.miette.dto;

import lombok.Data;

import java.util.List;

@Data
public class YamlGlossaryEntry {
    private String term;
    private String definition;
    private List<String> aliases;
}
