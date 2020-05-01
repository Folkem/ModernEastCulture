package com.folva.moderneastculture.model.dto;

import com.folva.moderneastculture.model.Repository;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Звичайний DTO-об'єкт коміксу
 */
public class Comics {

    /**
     * Типи коміксів
     */
    public enum Type {
        MANGA("manga"),
        RANOBE("ranobe");

        public final String name;

        Type(String name) {
            this.name = name;
        }
    }

    /**
     * Типи джерел коміксів
     */
    public enum Source {
        ORIGINAL("original"),
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
    private int chapterCount;
    private Source source;
    private int premiereYear;
    private Status status;
    private ArrayList<String> altNames;

    private final ArrayList<InvalidationListener> changeListeners;

    private Comics() {
        altNames = new ArrayList<>();
        changeListeners = new ArrayList<>();
    }

    /**
     * Надіслати всім слухачам повідомлення
     */
    public void invalidate() {
        changeListeners.forEach(invalidationListener ->
                invalidationListener.invalidated(new SimpleObjectProperty<>(this)));
    }

    /**
     * Додати нового слухача змін
     * @param listener новий слухач змін об'єкту
     */
    public void addInvalidationListener(InvalidationListener listener) {
        changeListeners.add(listener);
    }

    /**
     * @return ідентифікатор коміксу
     */
    public int getId() {
        return id;
    }

    /**
     * Встановлює новий ідентифікатор коміксу
     * @param id новий ідентифікатор коміксу
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return об'єкт автору
     */
    public Author getAuthor() {
        return author;
    }

    /**
     * Встановлює новий об'єкт автора
     * @param author новий об'єкт автора
     */
    public void setAuthor(Author author) {
        this.author = author;
    }

    /**
     * @return тип коміксу
     */
    public Type getType() {
        return type;
    }

    /**
     * Встановлює новий тип коміксу
     * @param type новий тип коміксу
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * @return назва коміксу
     */
    public String getName() {
        return name;
    }

    /**
     * Встановлює нову назву коміксу
     * @param name нова назва коміксу
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return опис коміксу
     */
    public String getDescription() {
        return description;
    }

    /**
     * Встановлює новий опис коміксу
     * @param description новий опис коміксу
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return кількість глав коміксу
     */
    public int getChapterCount() {
        return chapterCount;
    }

    /**
     * Встановлює нову кількість глав коміксу
     * @param chapterCount нова кількість глав коміксу
     */
    public void setChapterCount(int chapterCount) {
        this.chapterCount = chapterCount;
    }

    /**
     * @return тип джерела коміксу
     */
    public Source getSource() {
        return source;
    }

    /**
     * Встановлює новий тип джерела коміксу
     * @param source новий тип джерела коміксу
     */
    public void setSource(Source source) {
        this.source = source;
    }

    /**
     * @return рік початку випуску
     */
    public int getPremiereYear() {
        return premiereYear;
    }

    /**
     * Повертає рік початку випуску коміксу
     * @param premiereYear новий рік початку випуску
     */
    public void setPremiereYear(int premiereYear) {
        this.premiereYear = premiereYear;
    }

    /**
     * @return статус коміксу
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Встановлює новий статус коміксу
     * @param status новий статус коміксу
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * @return колекція альтернативних імен коміксу
     */
    public ArrayList<String> getAltNames() {
        return altNames;
    }

    /**
     * Встановлює нову колекцію альтернативних імен коміксу
     * @param altNames нова колекція альтернативних імен коміксу
     */
    public void setAltNames(ArrayList<String> altNames) {
        this.altNames = altNames;
    }

    /**
     * "Будувач" коміксу для більш елегантного конструювання об'єкту
     */
    public static class Builder {

        private Comics comics;

        private Builder() {
            comics = new Comics();
        }

        /**
         * Фабричний метод будівника
         * @return новий будувач
         */
        public static Builder newBuilder() {
            return new Builder();
        }

        /**
         * @param id ідентифікатор коміксу
         * @return цей будувач
         */
        public Builder setId(int id) {
            comics.id = id;
            return this;
        }

        /**
         * @param author автор коміксу
         * @return цей будувач
         */
        public Builder setAuthor(Author author) {
            comics.author = author;
            return this;
        }

        /**
         * @param type тип коміксу
         * @return цей будувач
         */
        public Builder setType(Type type) {
            comics.type = type;
            return this;
        }

        /**
         * @param name назва коміксу
         * @return цей будувач
         */
        public Builder setName(String name) {
            comics.name = name;
            return this;
        }

        /**
         * @param description опис коміксу
         * @return цей будувач
         */
        public Builder setDescription(String description) {
            comics.description = description;
            return this;
        }

        /**
         * @param chapterCount кількість глав коміксу
         * @return цей будувач
         */
        public Builder setChapterCount(int chapterCount) {
            comics.chapterCount = chapterCount;
            return this;
        }

        /**
         * @param source тип джерела коміксу
         * @return цей будувач
         */
        public Builder setSource(Source source) {
            comics.source = source;
            return this;
        }

        /**
         * @param premiereYear рік початку випуску коміксу
         * @return цей будувач
         */
        public Builder setPremiereYear(int premiereYear) {
            comics.premiereYear = Math.max(premiereYear, Repository.FIRST_MANGA_PREMIERE_YEAR);
            return this;
        }

        /**
         * @param status статус коміксу
         * @return цей будувач
         */
        public Builder setStatus(Status status) {
            comics.status = status;
            return this;
        }

        /**
         * @param altNames альтернативні назви коміксу
         * @return цей будувач
         */
        public Builder setAltNames(ArrayList<String> altNames) {
            comics.altNames = altNames;
            return this;
        }

        /**
         * @return побудований об'єкт коміксу
         */
        public Comics build() {
            Comics newComics = comics;
            comics = null;
            return newComics;
        }
    }

    @Override
    public boolean equals(Object obj) {
        boolean result = false;

        if (obj instanceof Comics) {
            Comics comics = (Comics) obj;
            boolean sameId = (id == comics.id);
            boolean sameAuthor = (Objects.equals(author, comics.author));
            boolean sameType = (Objects.equals(type, comics.type));
            boolean sameName = (Objects.equals(name, comics.name));
            boolean sameDescription = (Objects.equals(description, comics.description));
            boolean sameChapterCount = (chapterCount == comics.chapterCount);
            boolean sameSource = (Objects.equals(source, comics.source));
            boolean samePremiereYear = (premiereYear == comics.premiereYear);
            boolean sameStatus = (Objects.equals(status, comics.status));
            boolean sameAltNames = (Objects.equals(altNames, comics.altNames));
            result = sameId && sameAuthor && sameType && sameName
                    && sameDescription && sameChapterCount
                    && sameSource && samePremiereYear
                    && sameStatus && sameAltNames;
        }

        return result;
    }
}
