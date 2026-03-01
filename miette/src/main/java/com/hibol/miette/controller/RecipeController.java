package com.hibol.miette.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import com.hibol.miette.entity.Recipe;
import com.hibol.miette.entity.RecipeSearchIndex;
import com.hibol.miette.repository.RecipeRepository;
import com.hibol.miette.repository.RecipeSearchIndexRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;


@Controller
public class RecipeController {
    
    @Autowired private RecipeRepository recipeRepo;
    @Autowired private RecipeSearchIndexRepository searchIndexRepo;

    @GetMapping("/recettes")
    public String list(@RequestParam(required = false) String q,
                      @RequestParam(defaultValue = "0") int page,
                      Model model) {
        
        List<Recipe> recipes;
        
        if (q != null && !q.trim().isEmpty()) {
            // Recherche full-text
            List<RecipeSearchIndex> results = searchIndexRepo.search(q.trim() + "*"); // wildcard pour partial match
            List<Long> recipeIds = results.stream()
                .map(RecipeSearchIndex::getRecipeId)
                .collect(Collectors.toList());
            recipes = recipeRepo.findAllById(recipeIds);
        } else {
            // Toutes les recettes
            recipes = recipeRepo.findAll();
        }
        
        model.addAttribute("recipes", recipes);
        model.addAttribute("query", q);
        model.addAttribute("resultCount", recipes.size());
        model.addAttribute("isSearchMode", q != null && !q.trim().isEmpty());
        
        return "liste"; 
    }

    @GetMapping("/recette/{id}")
    public String detail(@PathVariable Long id, HttpServletRequest request, Model model) {
        Recipe recipe = recipeRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recette introuvable"));

        String referer = request.getHeader("Referer");
        HttpSession session = request.getSession();

        if (referer != null && referer.contains("/recettes")) {
            session.setAttribute("returnUrl", referer);
        }

        String returnUrl = (String) session.getAttribute("returnUrl");
        model.addAttribute("returnUrl", returnUrl != null ? returnUrl : "/recettes");

        model.addAttribute("recipe", recipe);
        return "recette";
    }

    @GetMapping("/admin/recette/{id}/delete")
    public String deleteRecipe(@PathVariable Long id, HttpServletRequest request, RedirectAttributes redirectAttributes, UriComponentsBuilder uriBuilder) {
        Recipe recipe = recipeRepo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recette introuvable"));
        
        String title = recipe.getTitle();
        recipeRepo.deleteById(id);
        
        // Retour intelligent : garde la recherche précédente
        String returnUrl = uriBuilder.replacePath("/recettes").query(request.getQueryString()).build().toString();
        redirectAttributes.addFlashAttribute("message", "✅ '" + title + "' supprimée !");
        
        return "redirect:" + returnUrl;
    }
}
