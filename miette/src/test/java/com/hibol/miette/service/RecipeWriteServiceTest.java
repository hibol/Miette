package com.hibol.miette.service;

import com.hibol.miette.dto.api.IngredientDto;
import com.hibol.miette.dto.api.PhaseDto;
import com.hibol.miette.dto.api.RecipeDto;
import com.hibol.miette.dto.api.StepDto;
import com.hibol.miette.entity.Phase;
import com.hibol.miette.entity.Recipe;
import com.hibol.miette.repository.IngredientRepository;
import com.hibol.miette.repository.RecipeRepository;
import com.hibol.miette.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires de RecipeWriteService.
 *
 * "@ExtendWith(MockitoExtension.class)" active Mockito pour cette classe :
 * les champs annotés "@Mock" seront de faux objets contrôlables,
 * et "@InjectMocks" crée le service en y injectant ces mocks automatiquement.
 *
 * Structure de chaque test : GIVEN / WHEN / THEN
 *   - GIVEN  : on prépare les données et on configure le comportement des mocks
 *   - WHEN   : on appelle la méthode testée
 *   - THEN   : on vérifie le résultat avec des assertions
 */
@ExtendWith(MockitoExtension.class)
class RecipeWriteServiceTest {

    // Les dépendances du service, remplacées par des mocks (faux objets)
    @Mock RecipeRepository recipeRepo;
    @Mock TagRepository tagRepo;
    @Mock IngredientRepository ingredientRepo;
    @Mock RecipeIndexingService indexingService;

    // Le vrai service, instancié avec les mocks ci-dessus
    @InjectMocks RecipeWriteService service;

    // ─────────────────────────────────────────────
    //  Méthodes utilitaires pour éviter la répétition
    // ─────────────────────────────────────────────

    /** Crée une recette simulée déjà existante en base (avec un id). */
    private Recipe recetteExistante(Long id) {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setTitle("Titre original");
        recipe.setCreatedAt(LocalDateTime.now());
        return recipe;
    }

    /**
     * Configure le mock du repository pour que save() renvoie simplement
     * l'objet qu'on lui passe (comportement réaliste sans vraie base).
     */
    private void mockSave() {
        when(recipeRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    /**
     * Configure le mock du repository d'ingrédients pour simuler
     * des ingrédients inconnus : findByLabel → vide, save → renvoie l'objet.
     */
    private void mockNouvelIngredient() {
        when(ingredientRepo.findByLabel(any())).thenReturn(Optional.empty());
        when(ingredientRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ─────────────────────────────────────────────
    //  Tests : update()
    // ─────────────────────────────────────────────

    /**
     * Cas principal du bug corrigé : une phase sans ingrédients ne doit pas
     * être silencieusement perdue lors de la sauvegarde.
     */
    @Test
    void update_deuxPhasesDontUneSansIngredients_lesDeuxSontSauvegardees() {
        // GIVEN
        when(recipeRepo.findById(1L)).thenReturn(Optional.of(recetteExistante(1L)));
        mockSave();
        mockNouvelIngredient();

        RecipeDto dto = new RecipeDto(1L, "Tarte aux pommes", null, null,
            List.of(),
            List.of(
                new PhaseDto(null, "Pâte", 1,
                    List.of(new IngredientDto(null, "Farine", 200.0, "g", 1)),
                    List.of()),
                new PhaseDto(null, "Variante sans gluten", 2,
                    List.of(),  // ← phase intentionnellement sans ingrédients
                    List.of(new StepDto(null, "Utiliser de la farine de riz", 1)))
            )
        );

        // WHEN
        service.update(1L, dto);

        // THEN
        // ArgumentCaptor capture l'objet passé à save() pour qu'on puisse l'inspecter
        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepo).save(captor.capture());

        assertThat(captor.getValue().getPhases())
            .as("Les deux phases doivent être présentes après sauvegarde")
            .hasSize(2);
    }

    @Test
    void update_unePhasePlusieursIngredients_tousLesIngredientsSontSauvegardes() {
        // GIVEN
        when(recipeRepo.findById(1L)).thenReturn(Optional.of(recetteExistante(1L)));
        mockSave();
        mockNouvelIngredient();

        RecipeDto dto = new RecipeDto(1L, "Quiche", null, null,
            List.of(),
            List.of(
                new PhaseDto(null, "", 1,
                    List.of(
                        new IngredientDto(null, "Œufs", 3.0, "pcs", 1),
                        new IngredientDto(null, "Crème", 200.0, "ml", 2),
                        new IngredientDto(null, "Lardons", 150.0, "g", 3)
                    ),
                    List.of())
            )
        );

        // WHEN
        service.update(1L, dto);

        // THEN
        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepo).save(captor.capture());

        Phase phase = captor.getValue().getPhases().iterator().next();
        assertThat(phase.getIngredientPhases())
            .as("Les 3 ingrédients doivent être présents")
            .hasSize(3);
    }

    @Test
    void update_unePhasePlusieursEtapes_toutesLesEtapesSontSauvegardees() {
        // GIVEN
        when(recipeRepo.findById(1L)).thenReturn(Optional.of(recetteExistante(1L)));
        mockSave();

        RecipeDto dto = new RecipeDto(1L, "Pain", null, null,
            List.of(),
            List.of(
                new PhaseDto(null, "", 1,
                    List.of(),
                    List.of(
                        new StepDto(null, "Pétrir", 1),
                        new StepDto(null, "Laisser lever", 2),
                        new StepDto(null, "Cuire", 3)
                    ))
            )
        );

        // WHEN
        service.update(1L, dto);

        // THEN
        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepo).save(captor.capture());

        Phase phase = captor.getValue().getPhases().iterator().next();
        assertThat(phase.getSteps())
            .as("Les 3 étapes doivent être présentes")
            .hasSize(3);
    }

    @Test
    void update_titreModifie_leNouveauTitreEstSauvegarde() {
        // GIVEN
        when(recipeRepo.findById(1L)).thenReturn(Optional.of(recetteExistante(1L)));
        mockSave();

        RecipeDto dto = new RecipeDto(1L, "Titre modifié", null, null, List.of(), List.of());

        // WHEN
        service.update(1L, dto);

        // THEN
        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepo).save(captor.capture());

        assertThat(captor.getValue().getTitle()).isEqualTo("Titre modifié");
    }

    @Test
    void update_recetteInexistante_leveUneException() {
        // GIVEN
        when(recipeRepo.findById(99L)).thenReturn(Optional.empty());

        RecipeDto dto = new RecipeDto(99L, "X", null, null, List.of(), List.of());

        // WHEN / THEN — on vérifie que le service lève bien une exception
        assertThatThrownBy(() -> service.update(99L, dto))
            .isInstanceOf(ResponseStatusException.class);
    }

    // ─────────────────────────────────────────────
    //  Tests : create()
    // ─────────────────────────────────────────────

    @Test
    void create_deuxPhases_lesDeuxSontCreees() {
        // GIVEN
        mockSave();

        RecipeDto dto = new RecipeDto(null, "Nouvelle recette", null, null,
            List.of(),
            List.of(
                new PhaseDto(null, "Phase principale", 1, List.of(), List.of()),
                new PhaseDto(null, "Variante", 2, List.of(), List.of())
            )
        );

        // WHEN
        service.create(dto);

        // THEN
        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepo).save(captor.capture());

        assertThat(captor.getValue().getPhases()).hasSize(2);
    }

    @Test
    void create_avecIngredientEtEtapes_toutEstPresent() {
        // GIVEN
        mockSave();
        mockNouvelIngredient();

        RecipeDto dto = new RecipeDto(null, "Soupe de carottes", null, null,
            List.of(),
            List.of(
                new PhaseDto(null, "", 1,
                    List.of(new IngredientDto(null, "Carottes", 3.0, "pcs", 1)),
                    List.of(
                        new StepDto(null, "Éplucher les carottes", 1),
                        new StepDto(null, "Faire bouillir", 2)
                    ))
            )
        );

        // WHEN
        service.create(dto);

        // THEN
        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepo).save(captor.capture());

        Phase phase = captor.getValue().getPhases().iterator().next();
        assertThat(phase.getIngredientPhases()).hasSize(1);
        assertThat(phase.getSteps()).hasSize(2);
    }

    @Test
    void create_leDateDeCreationEstRenseignee() {
        // GIVEN
        mockSave();

        RecipeDto dto = new RecipeDto(null, "Test", null, null, List.of(), List.of());

        // WHEN
        service.create(dto);

        // THEN
        ArgumentCaptor<Recipe> captor = ArgumentCaptor.forClass(Recipe.class);
        verify(recipeRepo).save(captor.capture());

        assertThat(captor.getValue().getCreatedAt())
            .as("La date de création doit être renseignée automatiquement")
            .isNotNull();
    }
}
