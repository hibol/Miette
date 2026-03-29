package com.hibol.miette.dto.api.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record GlossaryTermCreateDto(
        @NotBlank String term,
        @NotBlank String definition,
        List<String> aliases
) {}
