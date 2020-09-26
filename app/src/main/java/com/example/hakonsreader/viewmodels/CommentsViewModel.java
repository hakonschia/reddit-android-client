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

    private MutableLiveData<List<RedditComment>> comments;
    private MutableLiveData<Integer> itemsLoading;

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
     * @return The amount of items currently loading
     */
    public LiveData<Integer> getItemsLoading() {
        if (itemsLoading == null) {
            itemsLoading = new MutableLiveData<>();
            itemsLoading.setValue(0);
        }

        return itemsLoading;
    }

    public void loadComments(View parentLayout, RedditPost post) {
        itemsLoading.setValue(itemsLoading.getValue() + 1);

        App.getApi().getComments(post.getID(), (newComments -> {
            comments.setValue(newComments);

            itemsLoading.setValue(itemsLoading.getValue() - 1);
        }), ((code, t) -> {
            itemsLoading.setValue(itemsLoading.getValue() - 1);
            t.printStackTrace();

            Util.handleGenericResponseErrors(parentLayout, code, t);
        }));
    }

}
