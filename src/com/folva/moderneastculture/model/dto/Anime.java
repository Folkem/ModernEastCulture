package com.folva.moderneastculture.model.dto;

import com.folva.moderneastculture.model.Repository;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Звичайний DTO-об'єкт аніме
 */
public class Anime {

    /**
     * Типи аніме
     */
    public enum Type {
        SERIES("series"),
        MOVIE("movie");

        public final String name;

        Type(String name) {
            this.name = name;
        }
    }

    /**
     * Типи джерел аніме
     */
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
    private AgeRating ageRating;
    private int premiereYear;
    private YearSeason premiereSeason;
    private Status status;
    private ArrayList<String> altNames;

    private final ArrayList<InvalidationListener> changeListeners;

    private Anime() {
        altNames = new ArrayList<>();
        changeListeners = new ArrayList<>();
    }

    /**
     * Метод повідомлення слухачів змін стану
     */
    public void invalidate() {
        changeListeners.forEach(invalidationListener ->
                invalidationListener.invalidated(new SimpleObjectProperty<>(this)));
    }

    /**
     * Метод добавлення нових слухачів змін
     * @param listener новий слухач змін
     */
    public void addInvalidationListener(InvalidationListener listener) {
        changeListeners.add(listener);
    }

    /**
     * @return ідентифікатор аніме
     */
    public int getId() {
        return id;
    }

    /**
     * Встановлює новий ідентифікатор аніме
     * @param id новий ідентифікатор аніме
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return об'єкт автору аніме
     */
    public Author getAuthor() {
        return author;
    }

    /**
     * Встановлює новий об'єкт автору аніме
     * @param author новий об'єкт автору аніме
     */
    public void setAuthor(Author author) {
        this.author = author;
    }

    /**
     * @return тип аніме
     */
    public Type getType() {
        return type;
    }

    /**
     * Встановлює новий тип аніме
     * @param type новий тип аніме
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * @return назва аніме
     */
    public String getName() {
        return name;
    }

    /**
     * Встановлює нову назву аніме
     * @param name нова назва аніме
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return опис аніме
     */
    public String getDescription() {
        return description;
    }

    /**
     * Встановлює новий опис аніме
     * @param description новий опис аніме
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return кількість епізодів
     */
    public int getEpisodeCount() {
        return episodeCount;
    }

    /**
     * Встановлює нову кількість епізодів
     * @param episodeCount нова кількість епізодів
     */
    public void setEpisodeCount(int episodeCount) {
        this.episodeCount = episodeCount;
    }

    /**
     * @return тип джерела аніме
     */
    public Source getSource() {
        return source;
    }

    /**
     * Встановлює новий тип джерела аніме
     * @param source новий тип джерела аніме
     */
    public void setSource(Source source) {
        this.source = source;
    }

    /**
     * @return віковий рейтинг аніме
     */
    public AgeRating getAgeRating() {
        return ageRating;
    }

    /**
     * Встановлює новий віковий рейтинг аніме
     * @param rating новий віковий рейтинг аніме
     */
    public void setAgeRating(AgeRating rating) {
        this.ageRating = rating;
    }

    /**
     * @return рік початку випуску аніме
     */
    public int getPremiereYear() {
        return premiereYear;
    }

    /**
     * Встановлює новий рік початку випуску аніме
     * @param premiereYear новий рік початку випуску аніме
     */
    public void setPremiereYear(int premiereYear) {
        this.premiereYear = premiereYear;
    }

    /**
     * @return сезон випуску аніме
     */
    public YearSeason getPremiereSeason() {
        return premiereSeason;
    }

    /**
     * Встановлює новий сезон випуску аніме
     * @param premiereSeason новий сезон випуску аніме
     */
    public void setPremiereSeason(YearSeason premiereSeason) {
        this.premiereSeason = premiereSeason;
    }

    /**
     * @return статус аніме
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Встановлює новий статус аніме
     * @param status новий статус аніме
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * @return колекція альтернативних імен аніме
     */
    public ArrayList<String> getAltNames() {
        return altNames;
    }

    /**
     * Встановлює нову колекцію альтернативних імен аніме
     * @param altNames нова колекція альтернативних імен аніме
     */
    public void setAltNames(ArrayList<String> altNames) {
        this.altNames = altNames;
    }

    /**
     * Будувач аніме для більш елегантного та зрозумілого конструювання об'єкту аніме
     */
    public static class Builder {

        private Anime anime;

        private Builder() {
            anime = new Anime();
        }

        /**
         * @return новий об'єкт будівника
         */
        public static Builder newBuilder() {
            return new Builder();
        }

        /**
         * @param id ідентифікатор аніме
         * @return цей будівник
         */
        public Builder setId(int id) {
            anime.id = id;
            return this;
        }

        /**
         * @param author автор аніме
         * @return цей будівник
         */
        public Builder setAuthor(Author author) {
            anime.author = author;
            return this;
        }

        /**
         * @param type тип аніме
         * @return цей будівник
         */
        public Builder setType(Type type) {
            anime.type = type;
            return this;
        }

        /**
         * @param name назва аніме
         * @return цей будівник
         */
        public Builder setName(String name) {
            anime.name = name;
            return this;
        }

        /**
         * @param description опис аніме
         * @return цей будівник
         */
        public Builder setDescription(String description) {
            anime.description = description;
            return this;
        }

        /**
         * @param episodeCount кількість епізодів аніме
         * @return цей будівник
         */
        public Builder setEpisodeCount(int episodeCount) {
            anime.episodeCount = episodeCount;
            return this;
        }

        /**
         * @param source тип джерела аніме
         * @return цей будівник
         */
        public Builder setSource(Source source) {
            anime.source = source;
            return this;
        }

        /**
         * @param rating віковий рейтинг аніме
         * @return цей будівник
         */
        public Builder setAgeRating(AgeRating rating) {
            anime.ageRating = rating;
            return this;
        }

        /**
         * @param premiereYear рік початку випуску аніме
         * @return цей будівник
         */
        public Builder setPremiereYear(int premiereYear) {
            anime.premiereYear = Math.max(premiereYear, Repository.FIRST_ANIME_PREMIERE_YEAR);
            return this;
        }

        /**
         * @param season сезон початку випуску аніме
         * @return цей будівник
         */
        public Builder setPremiereSeason(YearSeason season) {
            anime.premiereSeason = season;
            return this;
        }

        /**
         * @param status статус аніме
         * @return цей будівник
         */
        public Builder setStatus(Status status) {
            anime.status = status;
            return this;
        }

        /**
         * @param altNames альтернативні назви аніме
         * @return цей будівник
         */
        public Builder setAltNames(ArrayList<String> altNames) {
            anime.altNames = altNames;
            return this;
        }

        /**
         * @return збудований об'єкт аніме
         */
        public Anime build() {
            Anime newAnime = anime;
            anime = null;
            return newAnime;
        }
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = false;

        if (obj instanceof Anime) {
            Anime anime = (Anime) obj;
            boolean sameId = (id == anime.id);
            boolean sameAuthor = (Objects.equals(author, anime.author));
            boolean sameType = (Objects.equals(type, anime.type));
            boolean sameName = (Objects.equals(name, anime.name));
            boolean sameDescription = (Objects.equals(description, anime.description));
            boolean sameEpisodeCount = (episodeCount == anime.episodeCount);
            boolean sameSource = (Objects.equals(source, anime.source));
            boolean sameRating = (Objects.equals(ageRating, anime.ageRating));
            boolean samePremiereYear = (premiereYear == anime.premiereYear);
            boolean samePremiereSeason = (Objects.equals(premiereSeason, anime.premiereSeason));
            boolean sameStatus = (Objects.equals(status, anime.status));
            boolean sameAltNames = (Objects.equals(altNames, anime.altNames));
            result = sameId && sameAuthor && sameType && sameName && sameDescription
                    && sameEpisodeCount && sameSource && sameRating && samePremiereYear
                    && samePremiereSeason && sameStatus && sameAltNames;
        }

        return result;
    }
}
