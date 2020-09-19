package com.example.hakonsreader.activites;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.interfaces.RedditListing;
import com.example.hakonsreader.constants.NetworkConstants;
import com.example.hakonsreader.databinding.ActivityReplyBinding;
import com.example.hakonsreader.misc.Util;

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
    }

    /**
     * Sends the reply
     *
     * @param view Ignored
     */
    public void sendReply(View view) {
        String text = binding.replyText.getText().toString();

        // TODO add text change listener and disable button if empty
        if (text.isEmpty()) {
            return;
        }
        // TODO get actual ID and thing instead of hardcoded values
        redditApi.postComment(text, "g5ttliv", Thing.COMMENT, comment -> {
            Log.d(TAG, "sendReply: Comment posted");
            // TODO send back comment as reply, add the comment back in the post
        }, (code, t) -> {
            Util.showGenericServerErrorSnackbar(binding.parentLayout);
        });

    }
}
