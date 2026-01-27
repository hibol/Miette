package com.hibol.miette.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "step")
public class Step {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Integer position;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phaseId", nullable = false)
    private Phase phase;
}
