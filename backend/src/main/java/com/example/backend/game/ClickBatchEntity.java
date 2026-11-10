package com.example.backend.game;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "click_batch")
public class ClickBatchEntity {

    @Id
    private UUID id;
    private Instant createdAt;

    protected ClickBatchEntity() {
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
