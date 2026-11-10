package com.example.backend.game;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.UUID;

public interface ClickBatchRepository extends JpaRepository<ClickBatchEntity, UUID> {

    @Modifying
    @Query(value = "INSERT INTO click_batch (id, created_at) VALUES (:id, :createdAt) ON CONFLICT (id) DO NOTHING",
            nativeQuery = true)
    int insertIfAbsent(UUID id, Instant createdAt);

    @Modifying
    @Query("delete from ClickBatchEntity b where b.createdAt < :cutoff")
    void deleteOlderThan(Instant cutoff);
}
