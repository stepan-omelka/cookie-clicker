package com.example.backend.game;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnedUpgradeRepository extends JpaRepository<OwnedUpgradeEntity, String> {
}
