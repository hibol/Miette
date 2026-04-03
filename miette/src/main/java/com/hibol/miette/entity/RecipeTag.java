package com.hibol.miette.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;

@Data
@Entity
@Table(name = "recipe_rel_tag")
public class RecipeTag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipeId", nullable = false)
    private Recipe recipe;

    @IndexedEmbedded
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tagId", nullable = false)
    private Tag tag;
}
