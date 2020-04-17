package com.folva.moderneastculture.model.dto;

public enum AgeRating {

    G(1), PG(2), PG13(3),
    R(4), NC17(5), UN(6);

    private final int id;

    AgeRating(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
