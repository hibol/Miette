package com.hibol.miette.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

@Data
@Entity
@Table(name = "ingredient")
public class Ingredient {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @FullTextField(analyzer = "ngram_index", searchAnalyzer = "ngram_search")
    @Column(nullable = false)
    private String label;
    
    @Column
    private String unit;

}
