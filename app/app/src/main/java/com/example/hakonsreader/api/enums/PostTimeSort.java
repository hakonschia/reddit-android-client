package com.example.hakonsreader.api.enums;

/**
 * Enum for posts that can be sorted by time
 */
public enum PostTimeSort {
    HOUR("hour"),
    DAY("day"),
    WEEK("week"),
    MONTH("month"),
    YEAR("year"),
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
