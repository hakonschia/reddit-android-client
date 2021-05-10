package com.example.hakonsreader.api.enums;

/**
 * A Reddit "Thing" (comment, post etc.)
 */
public enum Thing {
    COMMENT("t1"),
    ACCOUNT("t2"),
    POST("t3"),
    MESSAGE("t4"),
    SUBREDDIT("t5"),

    /**
     * Comments that are "2 more comments" type comments will be identified as MORE
     */
    MORE("more");


    private final String value;

    Thing(String value) {
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