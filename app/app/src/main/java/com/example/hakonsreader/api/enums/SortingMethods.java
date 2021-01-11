package com.example.hakonsreader.api.enums;

/**
 * Enum for how posts/comments can be sorted
 */
public enum SortingMethods {
    /**
     * For sorting by new
     */
    NEW("new"),

    /**
     * For sorting by hot (typically the standard sort)
     */
    HOT("hot"),

    /**
     * For sorting by top
     *
     * @see PostTimeSort
     */
    TOP("top"),

    /**
     * For sorting by controversial
     *
     * @see PostTimeSort
     */
    CONTROVERSIAL("controversial");

    private final String value;

    SortingMethods(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
