package com.example.hakonsreader.api.enums;

/**
 * Enum for posts that can be sorted by time
 */
public enum PostTimeSort {
    /**
     * Sort by the last hour (also sometimes called "Now")
     */
    HOUR("hour"),

    /**
     * Sort by the last day
     */
    DAY("day"),

    /**
     * Sort by the last week
     */
    WEEK("week"),

    /**
     * Sort by the last month
     */
    MONTH("month"),

    /**
     * Sort by the last year
     */
    YEAR("year"),

    /**
     * Sort by all time (for all posts, ie. no time filter)
     */
    ALL_TIME("all");


    private final String value;

    PostTimeSort(String value) {
        this.value = value;
    }

    /**
     * Retrieve the underlying string value of the sorting method. This string value corresponds
     * to how sorting is used with the Reddit API calls
     *
     * @return The string identifier for the thing (eg. "all")
     */
    public String getValue() {
        return value;
    }

}
