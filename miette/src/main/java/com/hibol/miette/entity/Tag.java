package com.hibol.miette.entity;


import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

@Data
@Entity
@Table(name = "tag")
public class Tag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @FullTextField
    @Column(nullable = false, unique = true)
    private String label;
}
