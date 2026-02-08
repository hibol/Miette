package com.hibol.miette.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hibol.miette.entity.Recipe;
import com.hibol.miette.entity.RecipeSearchIndex;
import com.hibol.miette.repository.RecipeRepository;
import com.hibol.miette.repository.RecipeSearchIndexRepository;


@Controller
@RequestMapping("/recettes")
public class RecipeController {
    
    @Autowired private RecipeRepository recipeRepo;
    @Autowired private RecipeSearchIndexRepository searchIndexRepo;

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                      @RequestParam(defaultValue = "0") int page,
                      Model model) {
        
        List<Recipe> recipes;
        
        if (q != null && !q.trim().isEmpty()) {
            // Recherche full-text
            List<RecipeSearchIndex> results = searchIndexRepo.search(q);
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
        
        return "recettes/liste"; 
    }
}
