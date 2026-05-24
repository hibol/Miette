package com.hibol.miette.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.hibol.miette.entity.Asset;
import com.hibol.miette.entity.IngredientPhase;
import com.hibol.miette.entity.Phase;
import com.hibol.miette.entity.Recipe;
import com.hibol.miette.entity.Step;
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
                       @RequestParam(required = false) String withArchived,
                       Authentication auth,
                       Model model) {

        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        List<Recipe> recipes = new ArrayList<>((q != null && !q.trim().isEmpty())
                ? recipeService.search(q.trim())
                : recipeService.findAllWithDetails());

        if (!isAdmin || withArchived == null) {
            recipes = recipes.stream().filter(r -> !r.isArchived()).collect(Collectors.toList());
        } else {
            recipes = recipes.stream().filter(Recipe::isArchived).collect(Collectors.toList());
        }

        boolean filterNotes = withNotes != null;
        if (filterNotes) recipes = recipeService.filterByAssetType(recipes, Asset.AssetType.NOTE);

        boolean filterPhotos = withPhotos != null;
        if (filterPhotos) recipes = recipeService.filterByAssetType(recipes, Asset.AssetType.PHOTO);

        Map<Long, LocalDateTime> activityDates = recipes.stream()
                .collect(Collectors.toMap(Recipe::getId, this::latestActivity));

        String canonicalUrl = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/recettes").replaceQuery(null).build().toUriString();

        model.addAttribute("recipes", recipes);
        model.addAttribute("activityDates", activityDates);
        model.addAttribute("ogUrl", canonicalUrl);
        model.addAttribute("query", q);
        model.addAttribute("resultCount", recipes.size());
        model.addAttribute("isSearchMode", q != null && !q.trim().isEmpty());
        model.addAttribute("withNotes", filterNotes);
        model.addAttribute("withPhotos", filterPhotos);
        model.addAttribute("withArchived", withArchived != null);
        model.addAttribute("isAdmin", isAdmin);
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
    public String detail(@PathVariable Long id, @RequestParam(required = false) String edit, HttpServletRequest request, Authentication auth, Model model) throws JsonProcessingException {
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Recipe recipe = recipeService.findByIdWithDetails(id)
            .filter(r -> isAdmin || !r.isArchived())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recette introuvable"));

        String referer = request.getHeader("Referer");
        HttpSession session = request.getSession();

        if (referer != null && referer.contains("/recettes")) {
            session.setAttribute("returnUrl", referer);
        }

        String returnUrl = (String) session.getAttribute("returnUrl");
        var coverAsset = recipe.getAssets().stream()
            .filter(ra -> ra.getAsset().getType() == Asset.AssetType.PHOTO && ra.isCover())
            .findFirst()
            .or(() -> recipe.getAssets().stream()
                .filter(ra -> ra.getAsset().getType() == Asset.AssetType.PHOTO)
                .findFirst());

        String ogImage    = coverAsset.map(ra -> storagePublicUrl + "/" + ra.getAsset().getThumbPath()).orElse(null);
        String fullImage  = coverAsset.map(ra -> storagePublicUrl + "/" + ra.getAsset().getPath()).orElse(null);
        String ogUrl      = ServletUriComponentsBuilder.fromCurrentRequest().replaceQuery(null).build().toUriString();

        model.addAttribute("recipeId", id);
        model.addAttribute("recipeTitle", recipe.getTitle());
        model.addAttribute("editMode", edit != null);
        if (ogImage != null) model.addAttribute("ogImage", ogImage);
        model.addAttribute("ogUrl", ogUrl);
        model.addAttribute("recipeJsonLd", buildJsonLd(recipe, fullImage));
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

    private String buildJsonLd(Recipe recipe, String ogImage) throws JsonProcessingException {
        Map<String, Object> ld = new LinkedHashMap<>();
        ld.put("@context", "https://schema.org");
        ld.put("@type", "Recipe");
        ld.put("name", recipe.getTitle());
        ld.put("author", Map.of("@type", "Person", "name", "Miette"));
        if (ogImage != null) ld.put("image", List.of(ogImage));
        if (recipe.getCreatedAt() != null)
            ld.put("datePublished", recipe.getCreatedAt().toLocalDate().toString());
        if (recipe.getUpdatedAt() != null)
            ld.put("dateModified", recipe.getUpdatedAt().toLocalDate().toString());

        List<String> ingredients = recipe.getPhases().stream()
            .sorted(Comparator.comparingInt(Phase::getPosition))
            .flatMap(p -> p.getIngredientPhases().stream()
                .sorted(Comparator.comparingInt(IngredientPhase::getPosition)))
            .map(this::formatIngredient)
            .collect(Collectors.toList());
        if (!ingredients.isEmpty()) ld.put("recipeIngredient", ingredients);

        List<Map<String, Object>> instructions = recipe.getPhases().stream()
            .sorted(Comparator.comparingInt(Phase::getPosition))
            .filter(p -> !p.getSteps().isEmpty())
            .map(p -> {
                Map<String, Object> section = new LinkedHashMap<>();
                section.put("@type", "HowToSection");
                section.put("name", p.getLabel());
                section.put("itemListElement", p.getSteps().stream()
                    .sorted(Comparator.comparingInt(Step::getPosition))
                    .map(s -> Map.of("@type", "HowToStep", "text", s.getLabel()))
                    .collect(Collectors.toList()));
                return section;
            })
            .collect(Collectors.toList());
        if (!instructions.isEmpty()) ld.put("recipeInstructions", instructions);

        return objectMapper.writeValueAsString(ld).replace("</", "<\\/");
    }

    private String formatIngredient(IngredientPhase ip) {
        List<String> parts = new ArrayList<>();
        if (ip.getQuantity() != null) {
            double q = ip.getQuantity();
            parts.add(q % 1 == 0 ? String.valueOf((int) q) : String.valueOf(q));
        }
        if (ip.getIngredient().getUnit() != null) parts.add(ip.getIngredient().getUnit());
        parts.add(ip.getIngredient().getLabel());
        return String.join(" ", parts);
    }
}
