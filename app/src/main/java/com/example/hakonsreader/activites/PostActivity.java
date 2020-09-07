package com.example.hakonsreader.activites;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.views.PostInfo;
import com.example.hakonsreader.views.VoteBar;
import com.google.gson.Gson;
import com.r0adkll.slidr.Slidr;

/**
 * Activity to show a Reddit post with its comments
 */
public class PostActivity extends AppCompatActivity {
    private PostInfo postInfo;
    private TextView comments;
    private VoteBar voteBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        Slidr.attach(this);

        this.initViews();

        RedditPost post = new Gson().fromJson(getIntent().getExtras().getString("post"), RedditPost.class);

        this.postInfo.setPost(post);
        this.voteBar.setPost(post);
    }

    /**
     * Initializes all the views of the activity
     */
    private void initViews() {
        this.postInfo = findViewById(R.id.post_info_comments);
        this.comments = findViewById(R.id.post_comments);
        this.voteBar = findViewById(R.id.vote_bar);
    }
}