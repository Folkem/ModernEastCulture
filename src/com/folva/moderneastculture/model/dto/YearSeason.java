package com.folva.moderneastculture.model.dto;

public enum YearSeason {
    WINTER("winter"), SPRING("spring"),
    SUMMER("summer"), AUTUMN("autumn"),
    UNDEFINED("undefined");

    public final String name;

    YearSeason(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
