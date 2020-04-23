package com.folva.moderneastculture.model.dto;

import com.folva.moderneastculture.controller.AnimePresentationControl;

public class AnimePair {

    private Anime key;

    public Anime getKey() { return key; }

    public void setKey(Anime key) {
        this.key = key;
    }

    private AnimePresentationControl value;

    public AnimePresentationControl getValue() { return value; }

    public void setValue(AnimePresentationControl value) {
        this.value = value;
    }

    public AnimePair(Anime key, AnimePresentationControl value) {
        this.key = key;
        this.value = value;
    }
}
