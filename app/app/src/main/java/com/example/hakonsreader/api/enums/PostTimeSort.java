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
     * Retrieve the underlying string value of the thing
     *
     * <p>This value can be used in addition to the things ID to create the fullname of the thing</p>
     *
     * <p>When creating the fullname use a "_" between the thing value and the ID</p>
     *
     * @return The string identifier for the thing (eg. "t1")
     */
    public String getValue() {
        return value;
    }

}
