package com.folva.moderneastculture.model.dto;

/**
 * Сезони року. Включається "неідентифікований" сезон року для
 * різних випадків
 */
public enum YearSeason {
    WINTER("winter"), SPRING("spring"),
    SUMMER("summer"), AUTUMN("autumn"),
    UNDEFINED("undefined");

    /**
     * Назва сезону в малому регістрі. Заміняє ланцюг методів
     * name().toLowerCase()
     */
    public final String name;

    YearSeason(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
