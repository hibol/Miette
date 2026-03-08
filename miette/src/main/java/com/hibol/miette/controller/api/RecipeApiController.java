package com.hibol.miette.controller.api;

import com.hibol.miette.dto.api.RecipeDto;
import com.hibol.miette.mapper.RecipeMapper;
import com.hibol.miette.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeApiController {

    private final RecipeService recipeService;
    private final RecipeMapper recipeMapper;

    @GetMapping
    public List<RecipeDto> list(@RequestParam(required = false) String q) {
        var recipes = (q != null && !q.trim().isEmpty())
            ? recipeService.search(q.trim())
            : recipeService.findAllWithDetails();
        return recipes.stream()
            .map(recipeMapper::toDto)
            .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeDto> get(@PathVariable Long id) {
        return recipeService.findByIdWithDetails(id)
            .map(recipeMapper::toDto)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<RecipeDto> create(@RequestBody RecipeDto dto) {
        // TODO point 5
        return ResponseEntity.status(501).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipeDto> update(@PathVariable Long id, @RequestBody RecipeDto dto) {
        // TODO point 5
        return ResponseEntity.status(501).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // TODO point 5
        return ResponseEntity.status(501).build();
    }
}