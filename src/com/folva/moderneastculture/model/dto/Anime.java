package com.folva.moderneastculture.model.dto;

import com.folva.moderneastculture.model.Repository;

public class Anime {

    public enum Type {
        SERIES,
        MOVIE
    }

    public enum Source {
        ORIGINAL,
        MANGA,
        RANOBE,
        OTHER
    }

    private int id;
    private Author author;
    private Type type;
    private String name;
    private String description;
    private int episodeCount;
    private Source source;
    private AgeRating rating;
    private int premiereYear;
    private YearSeason premiereSeason;
    private Status status;

    private Anime() {
    }

    public static class Builder {

        private Anime anime;

        private Builder() {
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder setId(int id) {
            anime.id = id;
            return this;
        }

        public Builder setAuthorId(Author author) {
            anime.author = author;
            return this;
        }

        public Builder setType(Type type) {
            anime.type = type;
            return this;
        }

        public Builder setName(String name) {
            anime.name = name;
            return this;
        }

        public Builder setDescription(String description) {
            anime.description = description;
            return this;
        }

        public Builder setEpisodeCount(int episodeCount) {
            anime.episodeCount = episodeCount;
            return this;
        }

        public Builder setSource(Source source) {
            anime.source = source;
            return this;
        }

        public Builder setRating(AgeRating rating) {
            anime.rating = rating;
            return this;
        }

        public Builder setPremiereYear(int premiereYear) {
            anime.premiereYear = Math.max(premiereYear, Repository.FIRST_ANIME_PREMIERE_YEAR);
            return this;
        }

        public Builder setPremiereSeason(YearSeason season) {
            anime.premiereSeason = season;
            return this;
        }

        public Builder setStatus(Status status) {
            anime.status = status;
            return this;
        }

        public Anime build() {
            Anime newAnime = anime;
            anime = null;
            return newAnime;
        }
    }

}
