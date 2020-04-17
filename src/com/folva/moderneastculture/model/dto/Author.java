package com.folva.moderneastculture.model.dto;

public class Author {

    public enum Type {
        STUDIO,
        HUMAN
    }

    private Integer id;
    private Type type;
    private String name;

    private Author() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static class Builder {

        private final Author author;

        private Builder() {
            author = new Author();
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder setId(int id) {
            if (id > 0) author.id = id;
            else author.id = null;

            return this;
        }

        public Builder setType(Type type) {
            author.type = type;
            return this;
        }

        public Builder setName(String name) {
            author.name = name;
            return this;
        }

        public Author build() {
            return author;
        }
    }
}
