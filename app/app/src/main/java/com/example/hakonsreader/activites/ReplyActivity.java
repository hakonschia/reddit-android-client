package com.example.hakonsreader.activites;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.model.RedditListing;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.ActivityReplyBinding;
import com.example.hakonsreader.misc.Util;
import com.google.gson.Gson;

public class ReplyActivity extends AppCompatActivity {
    private static final String TAG = "ReplyActivity";

    private static final String REPLY_TEXT = "replyText";


    private ActivityReplyBinding binding;

    private RedditApi redditApi = App.get().getApi();
    private RedditListing replyingTo;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = ActivityReplyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            binding.replyText.setText(savedInstanceState.getString(REPLY_TEXT));
        }

        // TODO get comment/post
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String jsonData = extras.getString(PostActivity.LISTING_KEY);
            String kind = extras.getString(PostActivity.KIND_KEY);

            if (kind.equals(Thing.POST.getValue())) {
                replyingTo = new Gson().fromJson(jsonData, RedditPost.class);
                Log.d(TAG, "onCreate: Replying to a post");
            } else if (kind.equals(Thing.COMMENT.getValue())) {
               // replyingTo = new Gson().fromJson(jsonData, RedditComment.class);
                Log.d(TAG, "onCreate: Replying to a comment");
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Store what is in the edit text
        outState.putString(REPLY_TEXT, binding.replyText.getText().toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
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
                    .putExtra(PostActivity.LISTING_KEY, new Gson().toJson(comment));

            setResult(RESULT_OK, data);
            finish();
        }, (code, t) -> {
            Util.handleGenericResponseErrors(binding.parentLayout, code, t);
        });

    }
}