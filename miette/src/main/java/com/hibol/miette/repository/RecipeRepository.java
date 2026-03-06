package com.hibol.miette.repository;

import java.util.List;
import java.util.Optional;
import com.hibol.miette.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    // Fetches tags + phases + ingredients in one query
    @Query("""
        SELECT DISTINCT r FROM Recipe r
        LEFT JOIN FETCH r.tags rt
        LEFT JOIN FETCH rt.tag
        LEFT JOIN FETCH r.phases p
        LEFT JOIN FETCH p.ingredientPhases ip
        LEFT JOIN FETCH ip.ingredient
        ORDER BY r.title ASC
        """)
    List<Recipe> findAllWithDetails();
    
    // For search results, we only want the matching recipes with their details
    @Query("""
        SELECT DISTINCT r FROM Recipe r
        LEFT JOIN FETCH r.tags rt
        LEFT JOIN FETCH rt.tag
        LEFT JOIN FETCH r.phases p
        LEFT JOIN FETCH p.ingredientPhases ip
        LEFT JOIN FETCH ip.ingredient
        WHERE r.id IN :ids
        ORDER BY r.title ASC
        """)
    List<Recipe> findAllWithDetailsByIds(@Param("ids") List<Long> ids);

    @Query("""
        SELECT DISTINCT r FROM Recipe r
        LEFT JOIN FETCH r.tags rt
        LEFT JOIN FETCH rt.tag
        LEFT JOIN FETCH r.phases p
        LEFT JOIN FETCH p.ingredientPhases ip
        LEFT JOIN FETCH ip.ingredient
        LEFT JOIN FETCH p.steps
        WHERE r.id = :id
        """)
    Optional<Recipe> findByIdWithDetails(@Param("id") Long id);
}
