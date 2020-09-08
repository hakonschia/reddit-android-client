package com.example.hakonsreader.api.model;

import java.util.List;

public class PostComments {

    List<Object> objects;

    static class Comment {

        private Data data;

        static class Data {

            private String id;
            private int score;

            private String body;

            List<PostComments> replies;
        }

    }


}
