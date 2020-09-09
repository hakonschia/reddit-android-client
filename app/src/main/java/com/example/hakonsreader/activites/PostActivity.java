package com.example.hakonsreader.activites;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.views.FullPostBar;
import com.example.hakonsreader.views.PostInfo;
import com.google.gson.Gson;
import com.r0adkll.slidr.Slidr;

import java.util.List;

/**
 * Activity to show a Reddit post with its comments
 */
public class PostActivity extends AppCompatActivity {
    private static final String TAG = "PostActivity";

    private RedditApi redditApi = RedditApi.getInstance(NetworkConstants.USER_AGENT);

    private PostInfo postInfo;
    private FullPostBar fullPostBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        Slidr.attach(this);

        this.initViews();

        RedditPost post = new Gson().fromJson(getIntent().getExtras().getString("post"), RedditPost.class);

        this.postInfo.setPost(post);
        this.fullPostBar.setPost(post);

        this.redditApi.getComments(post.getId(), (comments -> {
            int totalComments = 0;

            for (RedditComment comment : comments) {
                List<RedditComment> replies = comment.getReplies();

                if (replies != null) {
                    totalComments += replies.size();
                }
            }

            Log.d(TAG, "onCreate: # comments: " + totalComments);
        }), ((call, t) -> {
            Log.d(TAG, "onCreate: ERROR");
            t.printStackTrace();
        }));
    }

    /**
     * Initializes all the views of the activity
     */
    private void initViews() {
        this.postInfo = findViewById(R.id.post_info_comments);
        this.fullPostBar = findViewById(R.id.post_full_bar_comments);
    }
}