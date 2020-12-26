package com.example.hakonsreader.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.hakonsreader.activites.PostActivity;
import com.example.hakonsreader.api.enums.PostType;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.PostInfoBinding;
import com.example.hakonsreader.views.util.ViewUtil;
import com.google.gson.Gson;

import java.util.List;


/**
 * View for info about posts (title, author, subreddit etc)
 */
public class PostInfo extends ConstraintLayout {
    private static final String TAG = "PostInfo";

    private final PostInfoBinding binding;

    public PostInfo(@NonNull Context context) {
        this(context, null, 0, 0);
    }
    public PostInfo(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }
    public PostInfo(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }
    public PostInfo(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        binding = PostInfoBinding.inflate(LayoutInflater.from(context), this, true);
    }

    /**
     * Sets the post to use in this VoteBar and sets the initial state of the vote status
     *
     * @param post The post to set
     */
    public void setPost(@NonNull RedditPost post) {
        binding.setPost(post);
        binding.setIsCrosspost(post.getPostType() == PostType.CROSSPOST);
        binding.userReportsTitle.setOnClickListener(v -> ViewUtil.openReportsBottomSheet(post, getContext(), ignored -> binding.invalidateAll()));

        List<RedditPost> crossposts = post.getCrossposts();
        if (crossposts != null && crossposts.size() > 0) {
            RedditPost crosspost = crossposts.get(0);
            binding.setCrosspost(crosspost);
            binding.crosspostText.setOnClickListener(view -> this.openPost(crosspost));
        }
    }

    /**
     * Opens a post in a {@link PostActivity}
     *
     * @param post The post to open
     */
    private void openPost(RedditPost post) {
        Intent intent = new Intent(getContext(), PostActivity.class);
        intent.putExtra(PostActivity.POST_KEY, new Gson().toJson(post));
        Activity activity = (Activity)getContext();
        activity.startActivity(intent);
    }
}
