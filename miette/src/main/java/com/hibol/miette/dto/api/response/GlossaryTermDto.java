package com.hibol.miette.dto.api.response;

import java.util.List;

public record GlossaryTermDto(Long id, String term, String definition, List<AliasDto> aliases) {
    public record AliasDto(Long id, String alias) {}
}
