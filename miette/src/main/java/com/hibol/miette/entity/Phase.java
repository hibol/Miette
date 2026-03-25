package com.hibol.miette.entity;

import java.util.Set;

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
    @OrderBy("position ASC")
    private Set<IngredientPhase> ingredientPhases = new java.util.HashSet<>();
    
    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL)
    @OrderBy("position ASC")
    private Set<Step> steps = new java.util.HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Phase other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
