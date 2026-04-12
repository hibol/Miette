package com.hibol.miette.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.hibol.miette.entity.Ingredient;

@Repository
public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    Optional<Ingredient> findByLabel(String label);

    List<Ingredient> findAllByOrderByLabelAsc();

    @Query("SELECT i FROM Ingredient i WHERE LOWER(i.label) LIKE '%farine%' OR i.glutenStrength IS NOT NULL ORDER BY i.label ASC")
    List<Ingredient> findFlourIngredients();

    @Query("SELECT i FROM Ingredient i WHERE i NOT IN (SELECT ip.ingredient FROM IngredientPhase ip)")
    List<Ingredient> findOrphans();
}
