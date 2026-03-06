package com.hibol.miette.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@Data
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "recipe")
public class Recipe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    // Relations
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL)
    @OrderBy("position ASC")
    private Set<Phase> phases = new java.util.HashSet<>();
    
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL)
    private Set<RecipeTag> tags = new java.util.HashSet<>();
    
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL)
    private Set<RecipeAsset> files = new java.util.HashSet<>();
}
