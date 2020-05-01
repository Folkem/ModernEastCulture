package com.folva.moderneastculture.model.dto;

/**
 * "Відкрита" версія класу {@link javafx.util.Pair}. Дає
 * можливість змінювати ключ та значення, проте типи, що логічно,
 * залишаються тими же
 * @param <K> Тип ключа
 * @param <V> Тип значення
 */
public final class OpenPair<K, V> {

    private K key;

    /**
     * @return ключ
     */
    public K getKey() { return key; }

    /**
     * Встановлює новий ключ з тим же типом або розширяющим
     * @param key ключ
     */
    public void setKey(K key) {
        this.key = key;
    }

    private V value;

    /**
     * @return значення
     */
    public V getValue() { return value; }

    /**
     * Встановлює нове значення з тим же типом або розширяющим
     * @param value нове значення
     */
    public void setValue(V value) {
        this.value = value;
    }

    /**
     * Відкрита пара "Ключ-значення" по типу тих, що зберігаються в
     * {@link java.util.Map}
     * @param key ключ
     * @param value значення
     */
    public OpenPair(K key, V value) {
        this.key = key;
        this.value = value;
    }
}
