package com.hibol.miette.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "recipe")
public class Recipe {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    // Relations
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL)
    private List<Phase> phases = new java.util.ArrayList<>();
    
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL)
    private List<RecipeTag> tags = new java.util.ArrayList<>();
    
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL)
    private List<RecipeFile> files = new java.util.ArrayList<>();
}
