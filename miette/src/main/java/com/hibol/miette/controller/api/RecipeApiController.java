package com.hibol.miette.controller.api;

import com.hibol.miette.dto.api.response.RecipeDto;
import com.hibol.miette.entity.Recipe;
import com.hibol.miette.entity.Tag;
import com.hibol.miette.mapper.RecipeMapper;
import com.hibol.miette.security.SecurityUtils;
import com.hibol.miette.service.RecipeService;
import com.hibol.miette.repository.TagRepository;
import com.hibol.miette.service.RecipeWriteService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;

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
    public List<RecipeDto> list(@RequestParam(required = false) String q, Authentication auth) {
        boolean isAdmin = SecurityUtils.isAdmin(auth);
        var recipes = (q != null && !q.trim().isEmpty())
            ? recipeService.search(q.trim())
            : (isAdmin ? recipeService.findAllWithDetails() : recipeService.findAllNonArchivedWithDetails());
        if (!isAdmin && q != null && !q.trim().isEmpty()) {
            recipes = recipes.stream().filter(r -> !r.isArchived()).toList();
        }
        return recipes.stream().map(recipeMapper::toDto).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<RecipeDto> get(@PathVariable Long id, Authentication auth) {
        boolean isAdmin = SecurityUtils.isAdmin(auth);
        return recipeService.findByIdWithDetails(id)
            .filter(r -> isAdmin || !r.isArchived())
            .map(recipeMapper::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        recipeWriteService.setArchived(id, true);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/unarchive")
    public ResponseEntity<Void> unarchive(@PathVariable Long id) {
        recipeWriteService.setArchived(id, false);
        return ResponseEntity.noContent().build();
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