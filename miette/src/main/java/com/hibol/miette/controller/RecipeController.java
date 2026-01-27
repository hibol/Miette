package com.hibol.miette.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.hibol.miette.repository.RecipeRepository;


@Controller
@RequestMapping("/recettes")
public class RecipeController {
    @Autowired private RecipeRepository recipeRepo;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("recipes", recipeRepo.findAll());
        return "recettes/liste"; 
    }
}
