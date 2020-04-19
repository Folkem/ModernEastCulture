package com.folva.moderneastculture.model.dto;

public enum AgeRating {

    G("G", 1), PG("PG", 2), PG13("PG-13", 3),
    R("R", 4), NC17("NC-17", 5), UN("UN", 6);

    public final String name;
    public final int id;

    AgeRating(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public static AgeRating valueOfId(int id) {
        AgeRating ageRating = null;

        for (AgeRating rating : AgeRating.values()) {
            if (rating.id == id) {
                ageRating = rating;
                break;
            }
        }

        return ageRating;
    }
}
