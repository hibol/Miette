package com.hibol.miette.service;

import com.hibol.miette.dto.api.request.GlossaryTermCreateDto;
import com.hibol.miette.dto.api.response.GlossaryTermDto;
import com.hibol.miette.entity.GlossaryAlias;
import com.hibol.miette.entity.GlossaryTerm;
import com.hibol.miette.repository.GlossaryTermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GlossaryService {

    private final GlossaryTermRepository termRepo;

    public List<GlossaryTermDto> findAll() {
        return termRepo.findAllByOrderByTermAsc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public GlossaryTermDto create(GlossaryTermCreateDto dto) {
        GlossaryTerm term = new GlossaryTerm();
        term.setTerm(dto.term().trim());
        term.setDefinition(dto.definition().trim());
        applyAliases(term, dto.aliases());
        return toDto(termRepo.save(term));
    }

    @Transactional
    public GlossaryTermDto update(Long id, GlossaryTermCreateDto dto) {
        GlossaryTerm term = termRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Terme introuvable"));
        term.setTerm(dto.term().trim());
        term.setDefinition(dto.definition().trim());
        term.getAliases().clear();
        applyAliases(term, dto.aliases());
        return toDto(termRepo.save(term));
    }

    @Transactional
    public void delete(Long id) {
        termRepo.deleteById(id);
    }

    private void applyAliases(GlossaryTerm term, List<String> aliases) {
        if (aliases == null) return;
        for (String a : aliases) {
            if (a == null || a.isBlank()) continue;
            GlossaryAlias alias = new GlossaryAlias();
            alias.setTerm(term);
            alias.setAlias(a.trim());
            term.getAliases().add(alias);
        }
    }

    private GlossaryTermDto toDto(GlossaryTerm t) {
        return new GlossaryTermDto(
                t.getId(),
                t.getTerm(),
                t.getDefinition(),
                t.getAliases().stream()
                        .map(a -> new GlossaryTermDto.AliasDto(a.getId(), a.getAlias()))
                        .toList()
        );
    }
}
