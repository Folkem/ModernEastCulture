package com.folva.moderneastculture.model.dto;

/**
 * Звичайний DTO-об'єкт жанру
 */
public class Genre {

    public final int id;
    public final String name;

    public Genre(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
