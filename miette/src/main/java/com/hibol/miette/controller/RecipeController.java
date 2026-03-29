package com.hibol.miette.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.hibol.miette.entity.Recipe;
import com.hibol.miette.service.RecipeService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;


@Controller
@RequiredArgsConstructor
public class RecipeController {

    @Value("${storage.public-url:}")
    private String storagePublicUrl;

    private final RecipeService recipeService;

    @GetMapping("/")
    public String home() {
        return "redirect:/recettes";
    }

    @GetMapping("/recettes")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) String withNotes,
                       @RequestParam(required = false) String withPhotos,
                       Model model) {

        List<Recipe> recipes = (q != null && !q.trim().isEmpty())
                ? recipeService.search(q.trim())
                : recipeService.findAllWithDetails();

        boolean filterNotes = withNotes != null;
        if (filterNotes) recipes = recipeService.filterByAssetType(recipes, com.hibol.miette.entity.Asset.AssetType.NOTE);

        boolean filterPhotos = withPhotos != null;
        if (filterPhotos) recipes = recipeService.filterByAssetType(recipes, com.hibol.miette.entity.Asset.AssetType.PHOTO);

        model.addAttribute("recipes", recipes);
        model.addAttribute("query", q);
        model.addAttribute("resultCount", recipes.size());
        model.addAttribute("isSearchMode", q != null && !q.trim().isEmpty());
        model.addAttribute("withNotes", filterNotes);
        model.addAttribute("withPhotos", filterPhotos);
        model.addAttribute("storagePublicUrl", storagePublicUrl);

        return "liste";
    }

    @GetMapping("/recette/{id}")
    public String detail(@PathVariable Long id, @RequestParam(required = false) String edit, HttpServletRequest request, Model model) {
        Recipe recipe = recipeService.findByIdWithDetails(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recette introuvable"));

        String referer = request.getHeader("Referer");
        HttpSession session = request.getSession();

        if (referer != null && referer.contains("/recettes")) {
            session.setAttribute("returnUrl", referer);
        }

        String returnUrl = (String) session.getAttribute("returnUrl");
        model.addAttribute("recipeId", id);
        model.addAttribute("editMode", edit != null);
        model.addAttribute("returnUrl", returnUrl != null ? returnUrl : "/recettes");
        return "recette";
    }

    @GetMapping("/a-propos")
    public String apropos() {
        return "a-propos";
    }

    @GetMapping("/recette/new")
    public String newRecipe(Model model) {
        model.addAttribute("recipeId", null);
        model.addAttribute("editMode", true);
        model.addAttribute("returnUrl", "/recettes");
        return "recette";
    }
}
