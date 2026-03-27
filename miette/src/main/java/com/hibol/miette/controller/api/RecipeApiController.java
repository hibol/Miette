package com.hibol.miette.controller.api;

import com.hibol.miette.dto.api.response.RecipeDto;
import com.hibol.miette.entity.Recipe;
import com.hibol.miette.entity.Tag;
import com.hibol.miette.mapper.RecipeMapper;
import com.hibol.miette.service.RecipeService;
import com.hibol.miette.repository.TagRepository;
import com.hibol.miette.service.RecipeWriteService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RecipeApiController {

    private final RecipeService recipeService;
    private final RecipeWriteService recipeWriteService;
    private final RecipeMapper recipeMapper;
    private final TagRepository tagRepo;

    @GetMapping
    @PreAuthorize("permitAll()")
    public List<RecipeDto> list(@RequestParam(required = false) String q) {
        var recipes = (q != null && !q.trim().isEmpty())
            ? recipeService.search(q.trim())
            : recipeService.findAllWithDetails();
        return recipes.stream()
            .map(recipeMapper::toDto)
            .toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<RecipeDto> get(@PathVariable Long id) {
        return recipeService.findByIdWithDetails(id)
            .map(recipeMapper::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipeDto> update(@PathVariable Long id, @Valid @RequestBody RecipeDto dto) {
        Recipe updated = recipeWriteService.update(id, dto);
        return ResponseEntity.ok(recipeMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recipeWriteService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<RecipeDto> create(@Valid @RequestBody RecipeDto dto) {
        Recipe created = recipeWriteService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(recipeMapper.toDto(created));
    }

    @GetMapping("/tags")
    @PreAuthorize("permitAll()")
    public List<String> tags() {
        return tagRepo.findAll().stream()
            .map(Tag::getLabel)
            .sorted()
            .toList();
    }
}