package com.example.hakonsreader.api.enums;

/**
 * Enum for how posts/comments can be sorted
 */
public enum SortingMethods {
    /**
     * For sorting by new
     */
    NEW,
    /**
     * For sorting by hot (typically the standard sort)
     */
    HOT,
    /**
     * For sorting by top
     *
     * @see PostTimeSort
     */
    TOP,
    /**
     * For sorting by controversial
     *
     * @see PostTimeSort
     */
    CONTROVERSIAL
}
