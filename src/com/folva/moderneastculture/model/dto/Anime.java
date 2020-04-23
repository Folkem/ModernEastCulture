package com.folva.moderneastculture.model.dto;

import com.folva.moderneastculture.model.Repository;

import java.util.ArrayList;
import java.util.Objects;

public class Anime {

    public enum Type {
        SERIES("series"),
        MOVIE("movie");

        public final String name;

        Type(String name) {
            this.name = name;
        }
    }

    public enum Source {
        ORIGINAL("original"),
        MANGA("manga"),
        RANOBE("ranobe"),
        OTHER("other");

        public final String name;

        Source(String name) {
            this.name = name;
        }
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
    private ArrayList<String> altNames;

    private Anime() {
        altNames = new ArrayList<>();
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

    public int getEpisodeCount() {
        return episodeCount;
    }

    public void setEpisodeCount(int episodeCount) {
        this.episodeCount = episodeCount;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public AgeRating getRating() {
        return rating;
    }

    public void setRating(AgeRating rating) {
        this.rating = rating;
    }

    public int getPremiereYear() {
        return premiereYear;
    }

    public void setPremiereYear(int premiereYear) {
        this.premiereYear = premiereYear;
    }

    public YearSeason getPremiereSeason() {
        return premiereSeason;
    }

    public void setPremiereSeason(YearSeason premiereSeason) {
        this.premiereSeason = premiereSeason;
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

        private Anime anime;

        private Builder() {
            anime = new Anime();
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public Builder setId(int id) {
            anime.id = id;
            return this;
        }

        public Builder setAuthor(Author author) {
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

        public Builder setAltNames(ArrayList<String> altNames) {
            anime.altNames = altNames;
            return this;
        }

        public Anime build() {
            Anime newAnime = anime;
            anime = null;
            return newAnime;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Anime)) return false;

        Anime anime = (Anime)obj;

        boolean sameId = (id == anime.id);
        boolean sameAuthor = (Objects.equals(author, anime.author));
        boolean sameType = (Objects.equals(type, anime.type));
        boolean sameName = (Objects.equals(name, anime.name));
        boolean sameDescription = (Objects.equals(description, anime.description));
        boolean sameEpisodeCount = (episodeCount == anime.episodeCount);
        boolean sameSource = (Objects.equals(source, anime.source));
        boolean sameRating = (Objects.equals(rating, anime.rating));
        boolean samePremiereYear = (premiereYear == anime.premiereYear);
        boolean samePremiereSeason = (Objects.equals(premiereSeason, anime.premiereSeason));
        boolean sameStatus = (Objects.equals(status, anime.status));
        boolean sameAltNames = (Objects.equals(altNames, anime.altNames));

        return sameId && sameAuthor && sameType && sameName && sameDescription
                && sameEpisodeCount && sameSource && sameRating && samePremiereYear
                && samePremiereSeason && sameStatus && sameAltNames;
    }
}
