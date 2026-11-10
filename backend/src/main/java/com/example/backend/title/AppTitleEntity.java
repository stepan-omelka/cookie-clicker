package com.example.backend.title;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity

@Table(name = "app_title")
public class AppTitleEntity {
    @Id
    private Long id;
    @Column
    private String text;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    protected AppTitleEntity() {}

    public AppTitleEntity(Long id, String text) {
        this.id = id;
        this.text = text;
    }
}
