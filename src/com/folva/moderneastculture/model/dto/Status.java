package com.folva.moderneastculture.model.dto;

/**
 * Статус аніме/коміксу. Зазвичай він або анонсований,
 * або виходить наразі, або вже закінчений
 */
public enum Status {
    ANNOUNCED("announced"),
    ONGOING("ongoing"),
    FINISHED("finished");

    /**
     * Назва статусу в малому регістрі. Заміняє ланцюг методів
     * name().toLowerCase()
     */
    public final String name;

    Status(String name) {
        this.name = name;
    }
}
