package com.example.hakonsreader.api.model;

public class RedditComment {

    private Data data;

    private static class Data extends ListingData {
        private String body;
    }

    public String getBody() {
        return this.data.body;
    }


}
