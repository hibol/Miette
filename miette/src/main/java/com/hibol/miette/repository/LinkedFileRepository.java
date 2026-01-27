package com.hibol.miette.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hibol.miette.entity.LinkedFile;

@Repository
public interface LinkedFileRepository extends JpaRepository<LinkedFile, Long> {
}
