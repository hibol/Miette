package com.hibol.miette.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hibol.miette.service.IngredientService;
import com.hibol.miette.service.RecipeIndexingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final RecipeIndexingService indexingService;
    private final IngredientService ingredientService;

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("orphanIngredients", ingredientService.findOrphans());
        return "admin";
    }

    @PostMapping("/admin/search/reindex")
    public String reindex(RedirectAttributes redirectAttributes) {
        indexingService.rebuildIndex();
        redirectAttributes.addFlashAttribute("message", "✅ Index de recherche reconstruit !");
        return "redirect:/recettes";
    }

    @PostMapping("/admin/ingredients/purge-orphans")
    public String purgeOrphanIngredients(RedirectAttributes redirectAttributes) {
        int count = ingredientService.deleteOrphans();
        redirectAttributes.addFlashAttribute("message", "✅ " + count + " ingrédient(s) orphelin(s) supprimé(s).");
        return "redirect:/admin";
    }
}