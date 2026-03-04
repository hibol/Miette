package com.hibol.miette.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hibol.miette.entity.Recipe;
import com.hibol.miette.repository.RecipeRepository;
import com.hibol.miette.service.RecipeIndexingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final RecipeRepository recipeRepo;
    private final RecipeIndexingService indexingService;

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    @PostMapping("/admin/recette/{id}/delete")
    public String deleteRecipe(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Recipe recipe = recipeRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recette introuvable"));

        String title = recipe.getTitle();
        recipeRepo.deleteById(id);
        indexingService.removeFromIndex(id);

        redirectAttributes.addFlashAttribute("message", "✅ '" + title + "' supprimée !");
        return "redirect:/recettes";
    }

    @PostMapping("/admin/search/reindex")
    public String reindex(RedirectAttributes redirectAttributes) {
        indexingService.rebuildIndex();
        redirectAttributes.addFlashAttribute("message", "✅ Index de recherche reconstruit !");
        return "redirect:/recettes";
    }
}