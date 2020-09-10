package com.example.hakonsreader.activites;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.recyclerviewadapters.CommentsAdapter;
import com.example.hakonsreader.views.FullPostBar;
import com.example.hakonsreader.views.LoadingIcon;
import com.example.hakonsreader.views.PostInfo;
import com.google.gson.Gson;
import com.r0adkll.slidr.Slidr;

/**
 * Activity to show a Reddit post with its comments
 */
public class PostActivity extends AppCompatActivity {
    private static final String TAG = "PostActivity";

    private RedditApi redditApi = RedditApi.getInstance(NetworkConstants.USER_AGENT);

    private LoadingIcon loadingIcon;
    private PostInfo postInfo;
    private FullPostBar fullPostBar;
    private RecyclerView commentsList;

    private CommentsAdapter commentsAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);
        Slidr.attach(this);

        this.initViews();
        this.setupCommentsList();

        RedditPost post = new Gson().fromJson(getIntent().getExtras().getString("post"), RedditPost.class);

        this.postInfo.setPost(post);
        this.fullPostBar.setPost(post);

        this.loadingIcon.increaseLoadCount();
        this.redditApi.getComments(post.getId(), (comments -> {
            this.commentsAdapter.addComments(comments);
            this.loadingIcon.decreaseLoadCount();
        }), ((call, t) -> {
            this.loadingIcon.decreaseLoadCount();
            t.printStackTrace();
        }));
    }

    /**
     * Initializes all the views of the activity
     */
    private void initViews() {
        this.loadingIcon = findViewById(R.id.loading_icon);
        this.postInfo = findViewById(R.id.post_info_comments);
        this.fullPostBar = findViewById(R.id.post_full_bar_comments);
        this.commentsList = findViewById(R.id.post_comments);
    }

    /**
     * Sets up {@link PostActivity#commentsList}
     */
    private void setupCommentsList() {
        this.commentsAdapter = new CommentsAdapter();
        this.commentsList.setAdapter(this.commentsAdapter);
        this.commentsList.setLayoutManager(new LinearLayoutManager(this));
    }
}