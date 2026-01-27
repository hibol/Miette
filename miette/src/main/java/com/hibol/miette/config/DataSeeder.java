package com.hibol.miette.config;

import java.io.InputStream;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

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
import com.hibol.miette.repository.IngredientRepository;
import com.hibol.miette.repository.RecipeRepository;
import com.hibol.miette.repository.TagRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final RecipeRepository recipeRepo;
    private final TagRepository tagRepo;
    private final IngredientRepository ingredientRepo;

    @Bean
    CommandLineRunner seed() {
        return args -> {
            if (recipeRepo.count() == 0) {
                log.info("🧑‍🍳 Seeding YAML recipes...");
                seedFromYaml();
                log.info("✅ {} recipes added", recipeRepo.count());
            } else {
                log.info("⏭️  Seeder skipped (DB already initialized)");
            }
        };
    }

    @Transactional
    private void seedFromYaml() {
        try {
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            InputStream yamlStream = new ClassPathResource("recipes.yaml").getInputStream();
            YamlRoot root = yamlMapper.readValue(yamlStream, YamlRoot.class);
            List<YamlRecipe> yamlRecipes = root.getRecipes();

            for (YamlRecipe yamlRecipe : yamlRecipes) {
                Recipe recipe = new Recipe();
                recipe.setTitle(yamlRecipe.getTitle());

                // Tags
                if (yamlRecipe.getTags() != null) {
                    for (String tagLabel : yamlRecipe.getTags()) {
                        Tag tag = tagRepo.findByLabel(tagLabel).orElseGet(() -> {
                            Tag newTag = new Tag();
                            newTag.setLabel(tagLabel);
                            return tagRepo.save(newTag);
                        });
                        RecipeTag recipeTag = new RecipeTag();
                        recipeTag.setRecipe(recipe);
                        recipeTag.setTag(tag);
                        recipe.getTags().add(recipeTag);
                    }
                }

                // Phases (si présentes)
                if (yamlRecipe.getPhases() != null) {
                    for (YamlPhase yamlPhase : yamlRecipe.getPhases()) {
                        Phase phase = new Phase();
                        phase.setLabel(yamlPhase.getLabel());
                        phase.setPosition(yamlPhase.getPosition());
                        phase.setRecipe(recipe);
                        
                        // Ingrédients phase
                        if (yamlPhase.getIngredients() != null) {
                            for (YamlIngredient yIng : yamlPhase.getIngredients()) {
                                Ingredient ingredient = ingredientRepo.findByLabel(yIng.getLabel()).orElseGet(() -> {
                                    Ingredient newIngredient = new Ingredient();
                                    newIngredient.setLabel(yIng.getLabel());
                                    newIngredient.setUnit(yIng.getUnit());
                                    return ingredientRepo.save(newIngredient);
                                });
                                IngredientPhase ingPhase = new IngredientPhase();
                                ingPhase.setIngredient(ingredient);
                                ingPhase.setPhase(phase);
                                ingPhase.setQuantity(yIng.getQuantity());
                                phase.getIngredientPhases().add(ingPhase);
                            }
                        }

                        // Steps phase
                        if (yamlPhase.getSteps() != null) {
                            for (int i = 0; i < yamlPhase.getSteps().size(); i++) {
                                Step step = new Step();
                                step.setPosition(i + 1);
                                step.setLabel(yamlPhase.getSteps().get(i));
                                step.setPhase(phase);
                                phase.getSteps().add(step);
                            }
                        }

                        recipe.getPhases().add(phase);
                    }
                } else {
                    Phase phase = new Phase();
                    phase.setLabel("");
                    phase.setPosition(1);
                    phase.setRecipe(recipe);
                    recipe.getPhases().add(phase);

                    // Ingrédients globaux (sans phase)
                    if (yamlRecipe.getIngredients() != null) {
                        for (YamlIngredient yIng : yamlRecipe.getIngredients()) {
                            Ingredient ingredient = ingredientRepo.findByLabel(yIng.getLabel()).orElseGet(() -> {
                                Ingredient newIngredient = new Ingredient();
                                newIngredient.setLabel(yIng.getLabel());
                                newIngredient.setUnit(yIng.getUnit());
                                return ingredientRepo.save(newIngredient);
                            });
                            IngredientPhase ingPhase = new IngredientPhase();
                            ingPhase.setIngredient(ingredient);
                            ingPhase.setPhase(phase);
                            ingPhase.setQuantity(yIng.getQuantity());
                            phase.getIngredientPhases().add(ingPhase);
                        }
                    }

                    // Étapes globales
                    if (yamlRecipe.getSteps() != null) {
                        for (int i = 0; i < yamlRecipe.getSteps().size(); i++) {
                            Step step = new Step();
                            step.setPosition(i + 1);
                            step.setLabel(yamlRecipe.getSteps().get(i));
                            step.setPhase(phase);
                            phase.getSteps().add(step);
                        }
                    }
                }

                recipeRepo.save(recipe);
            }
        } catch (Exception e) {
            log.error("❌ Erreur seeding YAML", e);
        }
    }
}
