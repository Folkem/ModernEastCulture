package com.folva.moderneastculture.model.dto;

import com.folva.moderneastculture.model.Repository;

public class Comics {

    public enum Type {
        MANGA,
        RANOBE
    }

    public enum Source {
        ORIGINAL,
        RANOBE,
        OTHER
    }

    private int id;
    private Author author;
    private Type type;
    private String name;
    private String description;
    private int chapterCount;
    private Source source;
    private int premiereYear;
    private Status status;

    private Comics() {
    }

    public static class Builder {

        private Comics comics;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder setId(int id) {
            comics.id = id;
            return this;
        }

        public Builder setAuthorId(Author author) {
            comics.author = author;
            return this;
        }

        public Builder setType(Type type) {
            comics.type = type;
            return this;
        }

        public Builder setName(String name) {
            comics.name = name;
            return this;
        }

        public Builder setDescription(String description) {
            comics.description = description;
            return this;
        }

        public Builder setChapterCount(int chapterCount) {
            comics.chapterCount = chapterCount;
            return this;
        }

        public Builder setSource(Source source) {
            comics.source = source;
            return this;
        }

        public Builder setPremiereYear(int premiereYear) {
            comics.premiereYear = Math.max(premiereYear, Repository.FIRST_MANGA_PREMIERE_YEAR);
            return this;
        }

        public Builder setStatus(Status status) {
            comics.status = status;
            return this;
        }

        public Comics build() {
            Comics newComics = comics;
            comics = null;
            return newComics;
        }
    }
}
