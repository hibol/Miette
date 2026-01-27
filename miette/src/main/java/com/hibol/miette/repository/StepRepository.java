package com.hibol.miette.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hibol.miette.entity.Step;

@Repository
public interface StepRepository extends JpaRepository<Step, Long> {
}
