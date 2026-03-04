package com.hibol.miette.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.hibol.miette.dto.RecipeIndexDto;
import com.hibol.miette.entity.Recipe;
import com.hibol.miette.entity.Step;
import com.hibol.miette.entity.RecipeSearchIndex;
import com.hibol.miette.repository.RecipeRepository;
import com.hibol.miette.repository.RecipeSearchIndexRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeIndexingService {

    private final RecipeRepository recipeRepo;
    private final RecipeSearchIndexRepository searchIndexRepo;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<RecipeIndexDto> getAllForIndexing() {
        return recipeRepo.findAll().stream()
            .map(this::buildIndexDto)
            .toList();
    }

    @Transactional
    public void rebuildIndex() {
        log.info("🔍 Rebuilding search index...");
        searchIndexRepo.deleteAll();

        recipeRepo.findAll().forEach(recipe -> {
            RecipeSearchIndex index = new RecipeSearchIndex();
            index.setRecipeId(recipe.getId());
            index.setSearchContent(buildSearchContent(buildIndexDto(recipe)));
            searchIndexRepo.save(index);
        });

        ensureFullTextIndex();
        log.info("✅ Search index rebuilt ({} recipes)", searchIndexRepo.count());
    }

    @Transactional
    public void indexRecipe(Long recipeId) {
        recipeRepo.findById(recipeId).ifPresent(recipe -> {
            RecipeSearchIndex index = searchIndexRepo.findById(recipeId)
                .orElse(new RecipeSearchIndex());
            index.setRecipeId(recipeId);
            index.setSearchContent(buildSearchContent(buildIndexDto(recipe)));
            searchIndexRepo.save(index);
        });
    }

    @Transactional
    public void removeFromIndex(Long recipeId) {
        searchIndexRepo.deleteById(recipeId);
    }

    private void ensureFullTextIndex() {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM information_schema.STATISTICS
            WHERE table_schema = DATABASE()
            AND table_name = 'recipe_search_index'
            AND index_type = 'FULLTEXT'
            """, Integer.class);

        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE recipe_search_index ADD FULLTEXT(search_content)");
            log.info("✅ FULLTEXT index created");
        } else {
            log.info("⏭️  FULLTEXT index already exists, skipping");
        }
    }

    private String buildSearchContent(RecipeIndexDto dto) {
        return String.join(" ",
            dto.title(),
            String.join(" ", dto.tags()),
            String.join(" ", dto.ingredients()),
            String.join(" ", dto.steps())
        );
    }

    private RecipeIndexDto buildIndexDto(Recipe recipe) {
        Set<String> tags = recipe.getTags().stream()
            .map(rt -> rt.getTag().getLabel())
            .collect(Collectors.toSet());

        Set<String> ingredients = recipe.getPhases().stream()
            .flatMap(p -> p.getIngredientPhases().stream())
            .map(ip -> ip.getIngredient().getLabel())
            .collect(Collectors.toSet());

        Set<String> steps = recipe.getPhases().stream()
            .flatMap(p -> p.getSteps().stream())
            .map(Step::getLabel)
            .collect(Collectors.toSet());

        return new RecipeIndexDto(recipe.getId(), recipe.getTitle(), tags, ingredients, steps);
    }
}

