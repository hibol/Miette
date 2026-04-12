package com.hibol.miette.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hibol.miette.entity.Recipe;
import com.hibol.miette.mapper.RecipeMapper;
import com.hibol.miette.service.AppSettingService;
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
    private final AppSettingService appSettingService;
    private final RecipeMapper recipeMapper;
    private final ObjectMapper objectMapper;

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

        List<Recipe> recipes = new ArrayList<>((q != null && !q.trim().isEmpty())
                ? recipeService.search(q.trim())
                : recipeService.findAllWithDetails());

        boolean filterNotes = withNotes != null;
        if (filterNotes) recipes = recipeService.filterByAssetType(recipes, com.hibol.miette.entity.Asset.AssetType.NOTE);

        boolean filterPhotos = withPhotos != null;
        if (filterPhotos) recipes = recipeService.filterByAssetType(recipes, com.hibol.miette.entity.Asset.AssetType.PHOTO);

        Map<Long, LocalDateTime> activityDates = recipes.stream()
                .collect(Collectors.toMap(Recipe::getId, this::latestActivity));

        model.addAttribute("recipes", recipes);
        model.addAttribute("activityDates", activityDates);
        model.addAttribute("query", q);
        model.addAttribute("resultCount", recipes.size());
        model.addAttribute("isSearchMode", q != null && !q.trim().isEmpty());
        model.addAttribute("withNotes", filterNotes);
        model.addAttribute("withPhotos", filterPhotos);
        model.addAttribute("storagePublicUrl", storagePublicUrl);

        return "liste";
    }

    private LocalDateTime latestActivity(Recipe recipe) {
        LocalDateTime base = recipe.getUpdatedAt() != null ? recipe.getUpdatedAt()
            : (recipe.getCreatedAt() != null ? recipe.getCreatedAt() : LocalDateTime.MIN);
        return recipe.getAssets().stream()
            .map(ra -> ra.getAsset().getDate())
            .filter(d -> d != null)
            .max(Comparator.naturalOrder())
            .map(d -> d.isAfter(base) ? d : base)
            .orElse(base);
    }

    @GetMapping("/recette/{id}")
    public String detail(@PathVariable Long id, @RequestParam(required = false) String edit, HttpServletRequest request, Model model) throws JsonProcessingException {
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
        model.addAttribute("standardKeywords", appSettingService.getValue("standard_ingredient_keywords", "farine,eau,sel,levain,levure,lait"));
        model.addAttribute("recipeJson", objectMapper.writeValueAsString(recipeMapper.toDto(recipe)));
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
        model.addAttribute("standardKeywords", appSettingService.getValue("standard_ingredient_keywords", "farine,eau,sel,levain,levure,lait"));
        model.addAttribute("recipeJson", null);
        return "recette";
    }
}
