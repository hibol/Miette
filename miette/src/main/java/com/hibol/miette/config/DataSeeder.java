package com.hibol.miette.config;

import java.io.InputStream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.hibol.miette.dto.YamlIngredient;
import com.hibol.miette.dto.YamlPhase;
import com.hibol.miette.dto.YamlRecipe;
import com.hibol.miette.dto.YamlRoot;
import com.hibol.miette.entity.Ingredient;
import com.hibol.miette.entity.IngredientPhase;
import com.hibol.miette.entity.Phase;
import com.hibol.miette.entity.Recipe;
import com.hibol.miette.entity.RecipeTag;
import com.hibol.miette.entity.Step;
import com.hibol.miette.entity.Tag;
import com.hibol.miette.entity.User;
import com.hibol.miette.repository.IngredientRepository;
import com.hibol.miette.repository.RecipeRepository;
import com.hibol.miette.repository.TagRepository;
import com.hibol.miette.repository.UserRepository;
import com.hibol.miette.service.RecipeIndexingService;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class DataSeeder {

    private final RecipeRepository recipeRepo;
    private final TagRepository tagRepo;
    private final IngredientRepository ingredientRepo;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RecipeIndexingService indexingService;

    @Value("${miette.admin.password:}")
    private String adminPassword;

    @Value("${miette.admin.username:hibol}")
    private String adminUsername;

    @Bean
    public CommandLineRunner seed() {
        return args -> {
            seedRecipes();
            seedAdmin();
        };
    }

    @Transactional
    public void seedRecipes() throws Exception {
        if (recipeRepo.count() > 0) {
            log.info("⏭️  Recipes already seeded, skipping");
            return;
        }

        log.info("🧑‍🍳 Seeding recipes from YAML...");
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        InputStream yamlStream = new ClassPathResource("recipes.yaml").getInputStream();
        YamlRoot root = yamlMapper.readValue(yamlStream, YamlRoot.class);

        for (YamlRecipe yamlRecipe : root.getRecipes()) {
            Recipe recipe = buildRecipe(yamlRecipe);
            recipeRepo.save(recipe);
        }

        indexingService.rebuildIndex();
        log.info("✅ {} recipes seeded", recipeRepo.count());
    }

    public void seedAdmin() {
        if (userRepository.findByUsername(adminUsername).isPresent()) {
            log.info("⏭️  Admin '{}' already exists, skipping", adminUsername);
            return;
        }

        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("⚠️  MIETTE_ADMIN_PASSWORD not set — admin user not created");
            return;
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(User.Role.ADMIN);
        userRepository.save(admin);
        log.info("✅ Admin '{}' created", adminUsername);
    }

    private Recipe buildRecipe(YamlRecipe yamlRecipe) {
        Recipe recipe = new Recipe();
        recipe.setTitle(yamlRecipe.getTitle());

        // Tags
        if (yamlRecipe.getTags() != null) {
            for (String tagLabel : yamlRecipe.getTags()) {
                Tag tag = tagRepo.findByLabel(tagLabel).orElseGet(() -> {
                    Tag t = new Tag();
                    t.setLabel(tagLabel);
                    return tagRepo.save(t);
                });
                RecipeTag recipeTag = new RecipeTag();
                recipeTag.setRecipe(recipe);
                recipeTag.setTag(tag);
                recipe.getTags().add(recipeTag);
            }
        }

        // Phases
        if (yamlRecipe.getPhases() != null) {
            for (YamlPhase yamlPhase : yamlRecipe.getPhases()) {
                recipe.getPhases().add(buildPhase(yamlPhase, recipe));
            }
        } else {
            recipe.getPhases().add(buildDefaultPhase(yamlRecipe, recipe));
        }

        return recipe;
    }

    private Phase buildPhase(YamlPhase yamlPhase, Recipe recipe) {
        Phase phase = new Phase();
        phase.setLabel(yamlPhase.getLabel());
        phase.setPosition(yamlPhase.getPosition());
        phase.setRecipe(recipe);

        if (yamlPhase.getIngredients() != null) {
            yamlPhase.getIngredients().forEach(yIng ->
                phase.getIngredientPhases().add(buildIngredientPhase(yIng, phase)));
        }

        if (yamlPhase.getSteps() != null) {
            for (int i = 0; i < yamlPhase.getSteps().size(); i++) {
                phase.getSteps().add(buildStep(yamlPhase.getSteps().get(i), i + 1, phase));
            }
        }

        return phase;
    }

    private Phase buildDefaultPhase(YamlRecipe yamlRecipe, Recipe recipe) {
        Phase phase = new Phase();
        phase.setLabel("");
        phase.setPosition(1);
        phase.setRecipe(recipe);

        if (yamlRecipe.getIngredients() != null) {
            yamlRecipe.getIngredients().forEach(yIng ->
                phase.getIngredientPhases().add(buildIngredientPhase(yIng, phase)));
        }

        if (yamlRecipe.getSteps() != null) {
            for (int i = 0; i < yamlRecipe.getSteps().size(); i++) {
                phase.getSteps().add(buildStep(yamlRecipe.getSteps().get(i), i + 1, phase));
            }
        }

        return phase;
    }

    private IngredientPhase buildIngredientPhase(YamlIngredient yIng, Phase phase) {
        Ingredient ingredient = ingredientRepo.findByLabel(yIng.getLabel()).orElseGet(() -> {
            Ingredient ing = new Ingredient();
            ing.setLabel(yIng.getLabel());
            ing.setUnit(yIng.getUnit());
            return ingredientRepo.save(ing);
        });

        IngredientPhase ingPhase = new IngredientPhase();
        ingPhase.setIngredient(ingredient);
        ingPhase.setPhase(phase);
        ingPhase.setQuantity(yIng.getQuantity());
        return ingPhase;
    }

    private Step buildStep(String label, int position, Phase phase) {
        Step step = new Step();
        step.setLabel(label);
        step.setPosition(position);
        step.setPhase(phase);
        return step;
    }
}