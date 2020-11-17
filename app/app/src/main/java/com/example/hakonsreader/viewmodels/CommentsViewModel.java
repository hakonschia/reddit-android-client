package com.example.hakonsreader.viewmodels;


import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.hakonsreader.App;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.misc.Util;

import java.util.ArrayList;
import java.util.List;


/**
 * ViewModel for a list of comments on a Reddit post
 */
public class CommentsViewModel extends ViewModel {

    private final RedditApi api = App.get().getApi();

    private final MutableLiveData<RedditPost> post = new MutableLiveData<>();
    private final MutableLiveData<List<RedditComment>> comments = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loadingChange = new MutableLiveData<>();
    private final MutableLiveData<ErrorWrapper> error = new MutableLiveData<>();

    private String postId;

    /**
     * @param postId The ID of the Reddit post to retrieve comments for
     */
    public void setPostId(String postId) {
        this.postId = postId;
    }

    /**
     * @return A LiveData observable for the post. Retrieving comments also retrieves the updated
     * post information, and will be sent here
     */
    public LiveData<RedditPost> getPost() {
        return post;
    }

    /**
     * @return The comments
     */
    public LiveData<List<RedditComment>> getComments() {
        return comments;
    }

    /**
     * @return LiveData observable for potential errors from the API
     */
    public LiveData<ErrorWrapper> getError() {
        return error;
    }

    /**
     * Retrieve the value used for listening to when something has started or finished loading
     *
     * @return If something has started loading the value in this LiveData will be set to true, and when
     * it has finished loading it will be set to false
     */
    public LiveData<Boolean> onLoadingChange() {
        return loadingChange;
    }

    /**
     * Loads comments
     */
    public void loadComments() {
        loadingChange.setValue(true);

        api.post(postId).comments(newComments -> {
            comments.setValue(newComments);
            loadingChange.setValue(false);
        }, post::setValue, ((e, t) -> {
            t.printStackTrace();
            loadingChange.setValue(false);
            error.setValue(new ErrorWrapper(e, t));
        }));
    }

    /**
     * Loads more comments for the post
     *
     * @param comment The comment holding the child comments to load. {@link RedditComment#getKind()}
     *                has to return {@link com.example.hakonsreader.api.enums.Thing#MORE} for this
     *                to do anything
     * @param parent The parent comment. The "2 more comments" comment is automatically removed
     *               from the parent comment on successful requests
     */
    public void loadMoreComments(RedditComment comment, RedditComment parent) {
        loadingChange.setValue(true);
        api.post(postId).moreComments(comment.getChildren(), parent, newComments -> {
            List<RedditComment> dataSet = new ArrayList<>(comments.getValue());

            // Find the parent index to know where to insert the new comments
            int commentPos = dataSet.indexOf(comment);

            dataSet.remove(commentPos);
            dataSet.addAll(commentPos, newComments);

            if (parent != null) {
                parent.removeReply(comment);
            }

            comments.setValue(dataSet);
            loadingChange.setValue(false);
        }, (e, t) -> {
            error.setValue(new ErrorWrapper(e, t));
            loadingChange.setValue(false);
        });
    }

    /**
     * Loads comments from scratch. {@link CommentsViewModel#loadComments()} is automatically called
     */
    public void restart() {
        comments.setValue(new ArrayList<>());
        loadComments();
    }
}
