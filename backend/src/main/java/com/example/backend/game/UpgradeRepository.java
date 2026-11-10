package com.example.backend.game;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UpgradeRepository extends JpaRepository<UpgradeEntity, String> {

    List<UpgradeEntity> findAllByOrderBySortOrderAsc();
}
