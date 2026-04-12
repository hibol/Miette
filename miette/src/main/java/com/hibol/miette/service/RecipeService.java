package com.hibol.miette.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.search.mapper.orm.Search;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import com.hibol.miette.entity.Asset;
import com.hibol.miette.entity.IngredientPhase;
import com.hibol.miette.entity.Phase;
import com.hibol.miette.entity.Recipe;
import com.hibol.miette.entity.RecipeTag;
import com.hibol.miette.entity.Step;
import com.hibol.miette.repository.RecipeRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipeService {

    private final RecipeRepository recipeRepo;

    @PersistenceContext
    private EntityManager entityManager;

    public List<Recipe> findAllWithDetails() {
        return recipeRepo.findAllWithDetails();
    }

    private static final Pattern QUOTED_PHRASE = Pattern.compile("[\"\\u201C\\u201D]([^\"\\u201C\\u201D]+)[\"\\u201C\\u201D]");

    public List<Recipe> search(String query) {
        // Normaliser les guillemets typographiques en guillemets droits
        String normalized = query.replace('\u201C', '"').replace('\u201D', '"');

        // Extraire les phrases entre guillemets (min 3 chars pour matcher les n-grams)
        List<String> phrases = new ArrayList<>();
        Matcher m = QUOTED_PHRASE.matcher(normalized);
        StringBuffer luceneQuery = new StringBuffer();
        while (m.find()) {
            String phrase = m.group(1).trim();
            if (phrase.length() >= 3) phrases.add(phrase);
            // Remplacer "phrase exacte" par les mots seuls pour la requête Lucene
            m.appendReplacement(luceneQuery, Matcher.quoteReplacement(phrase));
        }
        m.appendTail(luceneQuery);

        String lucene = luceneQuery.toString().trim();
        if (lucene.isBlank() && phrases.isEmpty()) return List.of();

        List<Recipe> results;
        if (lucene.isBlank()) {
            // Phrase seule : pré-filtrer via les mots extraits
            String words = String.join(" ", phrases);
            results = Search.session(entityManager)
                .search(Recipe.class)
                .where(f -> f.simpleQueryString()
                    .fields("title", "tags.tag.label",
                            "phases.steps.label",
                            "phases.ingredientPhases.ingredient.label")
                    .matching(words))
                .fetchHits(500);
        } else {
            results = Search.session(entityManager)
                .search(Recipe.class)
                .where(f -> f.simpleQueryString()
                    .fields("title", "tags.tag.label",
                            "phases.steps.label",
                            "phases.ingredientPhases.ingredient.label")
                    .matching(lucene))
                .fetchHits(500);
        }

        // Post-filtre : chaque phrase doit apparaître dans au moins un champ
        if (!phrases.isEmpty()) {
            results = results.stream()
                .filter(r -> phrases.stream().allMatch(p -> recipeContainsPhrase(r, p)))
                .toList();
        }
        return results;
    }

    private boolean recipeContainsPhrase(Recipe recipe, String phrase) {
        String p = deaccent(phrase);
        if (deaccent(recipe.getTitle()).contains(p)) return true;
        for (RecipeTag rt : recipe.getTags())
            if (deaccent(rt.getTag().getLabel()).contains(p)) return true;
        for (Phase phase : recipe.getPhases()) {
            for (Step step : phase.getSteps())
                if (deaccent(step.getLabel()).contains(p)) return true;
            for (IngredientPhase ip : phase.getIngredientPhases())
                if (deaccent(ip.getIngredient().getLabel()).contains(p)) return true;
        }
        return false;
    }

    private String deaccent(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase();
    }

    public Optional<Recipe> findByIdWithDetails(Long id) {
        return recipeRepo.findByIdWithDetails(id);
    }

    public List<Recipe> filterByAssetType(List<Recipe> recipes, Asset.AssetType type) {
        return recipes.stream()
                .filter(r -> r.getAssets().stream().anyMatch(a -> a.getAsset().getType() == type))
                .toList();
    }
}
