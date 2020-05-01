package com.folva.moderneastculture.model.dto;

/**
 * Звичайний DTO-об'єкт автору
 */
public class Author {

    /**
     * Типи авторів
     */
    public enum Type {
        STUDIO,
        HUMAN
    }

    private int id;
    private Type type;
    private String name;

    private Author() {}

    /**
     * @return ідентифікатор автора
     */
    public int getId() {
        return id;
    }

    /**
     * Встановлює новий ідентифікатор автора
     * @param id новий ідентифікатор автора
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return тип автору
     */
    public Type getType() {
        return type;
    }

    /**
     * Встановлює новий тип автора
     * @param type новий тип автору
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * @return ім'я автору
     */
    public String getName() {
        return name;
    }

    /**
     * Встановлює нове ім'я автору
     * @param name нове ім'я автору
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (obj instanceof Author) {
            Author author = (Author) obj;
            boolean sameId = (id == author.id);
            boolean sameType = (type.equals(author.type));
            boolean sameName = (name.equals(author.name));
            result = sameId && sameType && sameName;
        }

        return result;
    }
}
