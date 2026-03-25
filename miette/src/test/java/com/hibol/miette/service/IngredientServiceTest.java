package com.hibol.miette.service;

import com.hibol.miette.entity.Ingredient;
import com.hibol.miette.repository.IngredientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock IngredientRepository ingredientRepo;

    @InjectMocks IngredientService ingredientService;

    @Test
    void findOrCreate_ingredientInconnu_estCreeEtSauvegarde() {
        // GIVEN — l'ingrédient n'existe pas encore en base
        when(ingredientRepo.findByLabel("Farine")).thenReturn(Optional.empty());
        when(ingredientRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        Ingredient result = ingredientService.findOrCreate("Farine", "g");

        // THEN
        ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
        verify(ingredientRepo).save(captor.capture());

        assertThat(captor.getValue().getLabel()).isEqualTo("Farine");
        assertThat(captor.getValue().getUnit()).isEqualTo("g");
        assertThat(result.getLabel()).isEqualTo("Farine");
    }

    @Test
    void findOrCreate_ingredientExistant_estRetourneSansSave() {
        // GIVEN — l'ingrédient existe déjà avec la même unité
        Ingredient existant = new Ingredient();
        existant.setLabel("Sel");
        existant.setUnit("pincée");
        when(ingredientRepo.findByLabel("Sel")).thenReturn(Optional.of(existant));

        // WHEN
        Ingredient result = ingredientService.findOrCreate("Sel", "pincée");

        // THEN — aucun save ne doit être appelé (unité identique, rien à faire)
        verify(ingredientRepo, never()).save(any());
        assertThat(result).isSameAs(existant);
    }

    @Test
    void findOrCreate_uniteModifiee_estMiseAJour() {
        // GIVEN — l'ingrédient existe mais avec une unité différente
        Ingredient existant = new Ingredient();
        existant.setLabel("Farine");
        existant.setUnit("g");
        when(ingredientRepo.findByLabel("Farine")).thenReturn(Optional.of(existant));
        when(ingredientRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // WHEN
        ingredientService.findOrCreate("Farine", "kg");

        // THEN — l'unité doit être mise à jour et sauvegardée
        ArgumentCaptor<Ingredient> captor = ArgumentCaptor.forClass(Ingredient.class);
        verify(ingredientRepo).save(captor.capture());

        assertThat(captor.getValue().getUnit())
            .as("L'unité doit avoir été mise à jour")
            .isEqualTo("kg");
    }
}
