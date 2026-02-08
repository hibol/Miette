package com.hibol.miette.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hibol.miette.entity.Asset;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
}
