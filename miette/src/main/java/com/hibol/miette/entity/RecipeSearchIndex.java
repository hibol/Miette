package com.hibol.miette.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "recipe_search_index")
public class RecipeSearchIndex {
    @Id
    private Long recipeId;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String searchContent;

    
    public Long getRecipeId() {
        return recipeId;
    }
    
    public void setRecipeId(Long id) {
        this.recipeId = id;
    }

    public void setSearchContent(String string) {
        this.searchContent = string;
    }
    
}
