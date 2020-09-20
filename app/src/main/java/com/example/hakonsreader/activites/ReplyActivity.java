package com.example.hakonsreader.activites;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.interfaces.RedditListing;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.databinding.ActivityReplyBinding;
import com.example.hakonsreader.misc.Util;
import com.google.gson.Gson;

public class ReplyActivity extends AppCompatActivity {
    private static final String TAG = "ReplyActivity";

    private ActivityReplyBinding binding;

    private RedditApi redditApi = RedditApi.getInstance(NetworkConstants.USER_AGENT);
    private RedditListing replyingTo;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = ActivityReplyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // TODO get comment/post
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String jsonData = extras.getString(PostActivity.LISTING);
            String kind = extras.getString(PostActivity.KIND);

            if (kind.equals(Thing.POST.getValue())) {
                replyingTo = new Gson().fromJson(jsonData, RedditPost.class);
                Log.d(TAG, "onCreate: Replying to a post");
            } else if (kind.equals(Thing.COMMENT.getValue())) {
                replyingTo = new Gson().fromJson(jsonData, RedditComment.class);
                Log.d(TAG, "onCreate: Replying to a comment");
            }
        }
    }

    /**
     * Sends the reply
     *
     * @param view Ignored
     */
    public void sendReply(View view) {
        String text = binding.replyText.getText().toString();

        // TODO add text change listener and disable button if empty
        if (text.isEmpty() || replyingTo == null) {
            return;
        }

        // TODO get actual ID and thing instead of hardcoded values
        redditApi.postComment(text, replyingTo, comment -> {
            Log.d(TAG, "sendReply: Comment posted");

            // Pass the new comment back and finish
            Intent data = new Intent()
                    .putExtra(PostActivity.LISTING, new Gson().toJson(comment));

            setResult(RESULT_OK, data);
            finish();
        }, (code, t) -> {
            Util.handleGenericResponseErrors(binding.parentLayout, code, t);
        });

    }
}
