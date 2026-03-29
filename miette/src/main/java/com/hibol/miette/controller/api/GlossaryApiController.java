package com.hibol.miette.controller.api;

import com.hibol.miette.dto.api.request.GlossaryTermCreateDto;
import com.hibol.miette.dto.api.response.GlossaryTermDto;
import com.hibol.miette.service.GlossaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/glossary")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class GlossaryApiController {

    private final GlossaryService glossaryService;

    @GetMapping
    @PreAuthorize("permitAll()")
    public List<GlossaryTermDto> list() {
        return glossaryService.findAll();
    }

    @PostMapping
    public ResponseEntity<GlossaryTermDto> create(@Valid @RequestBody GlossaryTermCreateDto dto) {
        return ResponseEntity.ok(glossaryService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GlossaryTermDto> update(@PathVariable Long id, @Valid @RequestBody GlossaryTermCreateDto dto) {
        return ResponseEntity.ok(glossaryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        glossaryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
