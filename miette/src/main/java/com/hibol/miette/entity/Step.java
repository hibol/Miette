package com.hibol.miette.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

@Data
@Entity
@Table(name = "step")
public class Step {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Integer position;
    
    @FullTextField(analyzer = "ngram_index", searchAnalyzer = "ngram_search")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phaseId", nullable = false)
    private Phase phase;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Step other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
