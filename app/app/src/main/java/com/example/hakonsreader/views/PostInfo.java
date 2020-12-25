package com.example.hakonsreader.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.databinding.BindingAdapter;
import androidx.fragment.app.FragmentManager;

import com.example.hakonsreader.activites.PostActivity;
import com.example.hakonsreader.api.enums.PostType;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.PostInfoBinding;
import com.example.hakonsreader.fragments.ReportsBottomSheet;
import com.example.hakonsreader.interfaces.OnReportsIgnoreChangeListener;
import com.example.hakonsreader.views.util.ViewUtil;
import com.google.gson.Gson;

import java.util.List;


/**
 * View for info about posts (title, author, subreddit etc)
 */
public class PostInfo extends ConstraintLayout {
    private static final String TAG = "PostInfo";

    private final PostInfoBinding binding;

    @Nullable
    private OnReportsIgnoreChangeListener onReportsIgnored;

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

    public void setOnReportsIgnored(@Nullable OnReportsIgnoreChangeListener onReportsIgnored) {
        this.onReportsIgnored = onReportsIgnored;
    }

    /**
     * Sets the post to use in this VoteBar and sets the initial state of the vote status
     *
     * @param post The post to set
     */
    public void setPost(@NonNull RedditPost post) {
        binding.setPost(post);
        binding.setIsCrosspost(post.getPostType() == PostType.CROSSPOST);
        binding.userReportsTitle.setOnClickListener(v -> openReportsBottomSheet(post));

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

    /**
     * Opens a bottom sheet for the reports, if the post has any reports
     *
     * @param post The post to open reports for
     */
    private void openReportsBottomSheet(RedditPost post) {
        if (post.getNumReports() == 0) {
            return;
        }
        FragmentManager manager = ((AppCompatActivity)getContext()).getSupportFragmentManager();

        ReportsBottomSheet bottomSheet = new ReportsBottomSheet();
        bottomSheet.setPost(post);
        bottomSheet.setOnIgnoreChange(ignored -> {
            // TODO look into BaseObservable, or something, so that changing the values will automatically
            //  reflect on the binding, so I don't have to set the post again like this
            binding.setPost(post);
            binding.executePendingBindings();
        });

        bottomSheet.show(manager, "reportsBottomSheet");
    }
}
