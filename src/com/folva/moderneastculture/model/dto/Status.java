package com.folva.moderneastculture.model.dto;

public enum Status {
    ANNOUNCED("announced"),
    ONGOING("ongoing"),
    FINISHED("finished");

    public final String type;

    Status(String type) {
        this.type = type;
    }
}
