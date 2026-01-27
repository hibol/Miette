package com.hibol.miette.entity;

import java.util.List;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "phase")
public class Phase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Integer position;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String label;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipeId", nullable = false)
    private Recipe recipe;

    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL)
    private List<IngredientPhase> ingredientPhases = new java.util.ArrayList<>();
    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL)
    private List<Step> steps = new java.util.ArrayList<>();
}
