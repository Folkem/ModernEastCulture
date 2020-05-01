package com.folva.moderneastculture.model.dto;

/**
 * Віковий рейтинг аніме/коміксу. Перераховані значення признані
 * світом, тому можуть використовуватися й для інших творів
 */
public enum AgeRating {

    G("G", 1), PG("PG", 2), PG13("PG-13", 3),
    R("R", 4), NC17("NC-17", 5), UN("UN", 6);

    /**
     * Більш коректна назва
     */
    public final String name;
    /**
     * Ідентифікатор в таблиці бази данних
     */
    public final int id;

    AgeRating(String name, int id) {
        this.name = name;
        this.id = id;
    }

    /**
     * @param id ідентифікатор шуканого об'єкту рейтингу
     * @return шуканий об'єкт рейтингу, якщо знайдений, або null, якщо ны
     */
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
