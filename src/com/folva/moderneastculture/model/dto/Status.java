package com.folva.moderneastculture.model.dto;

public enum Status {
    ANNOUNCED("announced"),
    ONGOING("ongoing"),
    FINISHED("finished");

    public final String name;

    Status(String name) {
        this.name = name;
    }
}
