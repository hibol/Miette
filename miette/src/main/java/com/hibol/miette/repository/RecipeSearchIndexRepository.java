package com.hibol.miette.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hibol.miette.entity.RecipeSearchIndex;

@Repository
public interface RecipeSearchIndexRepository extends JpaRepository<RecipeSearchIndex, Long> {
    @Query(value = "SELECT * FROM recipe_search_index WHERE MATCH(search_content) AGAINST(?1 IN BOOLEAN MODE)", nativeQuery = true)
    List<RecipeSearchIndex> search(@Param("query") String query);
}

