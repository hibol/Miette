package com.hibol.miette.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hibol.miette.entity.Phase;

@Repository
public interface PhaseRepository extends JpaRepository<Phase, Long> {
}
