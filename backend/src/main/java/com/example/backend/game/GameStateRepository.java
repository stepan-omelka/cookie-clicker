package com.example.backend.game;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface GameStateRepository extends JpaRepository<GameStateEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GameStateEntity g where g.id = :id")
    Optional<GameStateEntity> findByIdForUpdate(long id);
}
