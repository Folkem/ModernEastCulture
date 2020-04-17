package com.folva.moderneastculture.model.dto;

import com.folva.moderneastculture.model.Repository;

import java.util.ArrayList;

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
    private ArrayList<String> altNames;

    private Comics() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getChapterCount() {
        return chapterCount;
    }

    public void setChapterCount(int chapterCount) {
        this.chapterCount = chapterCount;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public int getPremiereYear() {
        return premiereYear;
    }

    public void setPremiereYear(int premiereYear) {
        this.premiereYear = premiereYear;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public ArrayList<String> getAltNames() {
        return altNames;
    }

    public void setAltNames(ArrayList<String> altNames) {
        this.altNames = altNames;
    }

    public static class Builder {

        private Comics comics;

        private Builder() {
            comics = new Comics();
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
