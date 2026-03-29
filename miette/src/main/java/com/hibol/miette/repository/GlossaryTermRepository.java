package com.hibol.miette.repository;

import com.hibol.miette.entity.GlossaryTerm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GlossaryTermRepository extends JpaRepository<GlossaryTerm, Long> {
    Optional<GlossaryTerm> findByTerm(String term);
    List<GlossaryTerm> findAllByOrderByTermAsc();
}
