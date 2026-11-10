package com.example.backend.title;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_title")
public class AppTitleEntity {

    @Id
    private Long id;
    private String text;

    protected AppTitleEntity() {
    }

    public AppTitleEntity(Long id, String text) {
        this.id = id;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}
