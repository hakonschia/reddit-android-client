package com.example.hakonsreader.viewmodels;

import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.misc.Util;

import java.util.ArrayList;
import java.util.List;

public class CommentsViewModel extends ViewModel {

    private List<RedditComment> commentsDataSet = new ArrayList<>();

    private MutableLiveData<RedditPost> post;
    private MutableLiveData<List<RedditComment>> comments;
    private MutableLiveData<Boolean> loadingChange;


    public LiveData<RedditPost> getPost() {
        if (post == null) {
            post = new MutableLiveData<>();
        }

        return post;
    }

    /**
     * @return The comments
     */
    public LiveData<List<RedditComment>> getComments() {
        if (comments == null) {
            comments = new MutableLiveData<>();
            comments.setValue(commentsDataSet);
        }

        return comments;
    }

    /**
     * Retrieve the value used for listening to when something has started or finished loading
     *
     * @return If something has started loading the value in this LiveData will be set to true, and when
     * it has finished loading it will be set to false
     */
    public LiveData<Boolean> onLoadingChange() {
        if (loadingChange == null) {
            loadingChange = new MutableLiveData<>();
        }

        return loadingChange;
    }

    public void loadComments(View parentLayout, String postId) {
        loadingChange.setValue(true);

        App.get().getApi().getComments(postId, newComments -> {
            comments.setValue(newComments);
            loadingChange.setValue(false);
        }, newPost -> {
            post.setValue(newPost);
        }, ((code, t) -> {
            t.printStackTrace();
            loadingChange.setValue(false);

            Util.handleGenericResponseErrors(parentLayout, code, t);
        }));
    }

}
