package com.example.hakonsreader.activites;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.enums.PostType;
import com.example.hakonsreader.api.enums.Thing;
import com.example.hakonsreader.api.interfaces.ReplyableRequest;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditListing;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.ActivityReplyBinding;
import com.example.hakonsreader.misc.InternalLinkMovementMethod;
import com.example.hakonsreader.misc.Util;
import com.google.gson.Gson;

import io.noties.markwon.Markwon;
import io.noties.markwon.editor.MarkwonEditor;
import io.noties.markwon.editor.MarkwonEditorTextWatcher;

public class ReplyActivity extends AppCompatActivity {
    private static final String TAG = "ReplyActivity";

    /**
     * Key used to store the state of the reply text
     */
    private static final String REPLY_TEXT = "replyText";

    /**
     * Key used to store if the URL dialog is shown
     */
    private static final String LINK_DIALOG_SHOWN = "urlDialogShown";
    /**
     * Key used to store the state of the URL dialog link text
     */
    private static final String LINK_DIALOG_TEXT = "urlDialogText";
    /**
     * Key used to store the state of the URL dialog link
     */
    private static final String LINK_DIALOG_LINK = "urlDialogLink";

    /**
     * The key used used to store if the confirm discard dialog is shown
     */
    private static final String CONFIRM_DIALOG_SHOWN = "confirmDialogShown";



    private ActivityReplyBinding binding;

    private final RedditApi redditApi = App.get().getApi();
    private RedditListing replyingTo;

    /**
     * Dialog displayed when the user wants to finish the activity with text in the input field
     * that ensures the user wants to discard the text
     */
    private Dialog confirmDiscardDialog;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReplyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            binding.markdownInput.setText(savedInstanceState.getString(REPLY_TEXT));

            // Restore state of the link dialog
            boolean showLinkDialog = savedInstanceState.getBoolean(LINK_DIALOG_SHOWN);
            if (showLinkDialog) {
                String text = savedInstanceState.getString(LINK_DIALOG_TEXT);
                String link = savedInstanceState.getString(LINK_DIALOG_LINK);
                binding.markdownInput.showLinkDialog(text, link);
            }

            boolean showConfirmDialog = savedInstanceState.getBoolean(CONFIRM_DIALOG_SHOWN);
            if (showConfirmDialog) {
                this.showConfirmDialog();
            }
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String jsonData = extras.getString(PostActivity.LISTING_KEY);
            String kind = extras.getString(PostActivity.KIND_KEY);

            if (Thing.POST.getValue().equals(kind)) {
                replyingTo = new Gson().fromJson(jsonData, RedditPost.class);

                RedditPost post = (RedditPost) replyingTo;

                // If the post is a text post, set the summary to the text post, otherwise the title of the post
                if (post.getPostType().equals(PostType.TEXT)) {
                    App.get().getMark().setMarkdown(binding.summary, post.getSelftext());
                } else {
                    binding.summary.setText(post.getTitle());
                }
            } else if (Thing.COMMENT.getValue().equals(kind)) {
                replyingTo = new Gson().fromJson(jsonData, RedditComment.class);

                App.get().getMark().setMarkdown(binding.summary, ((RedditComment)replyingTo).getBody());
            }

            binding.setListing(replyingTo);
        }

        // Set this link movement method so links work the same way in the preview as the rest of the app
        //binding.preview.setMovementMethod(InternalLinkMovementMethod.getInstance(this));

        this.showNotLoggedInDialogIfNotLoggedIn();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Store what is in the edit text
        outState.putString(REPLY_TEXT, binding.markdownInput.getInputText());

        // Store state of the link dialog
        if (binding.markdownInput.isLinkDialogShown()) {
            outState.putBoolean(LINK_DIALOG_SHOWN, true);

            String text = binding.markdownInput.getLinkDialogText();
            String link = binding.markdownInput.getLinkDialogLink();

            outState.putString(LINK_DIALOG_TEXT, text);
            outState.putString(LINK_DIALOG_LINK, link);

            // Ensure the dialog is dismissed or else it will cause a leak
            binding.markdownInput.dismissLinkDialog();
        } else {
            outState.putBoolean(LINK_DIALOG_SHOWN, false);
        }

        // Store state of the confirm discard dialog
        if (confirmDiscardDialog != null && confirmDiscardDialog.isShowing()) {
            outState.putBoolean(CONFIRM_DIALOG_SHOWN, true);

            confirmDiscardDialog.dismiss();
        } else {
            outState.putBoolean(CONFIRM_DIALOG_SHOWN, false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        App.get().setActiveActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    /**
     * If there is text a dialog is shown to warn the user that they are leaving text behind
     * and makes the user confirm they want to discard the text
     */
    @Override
    public void finish() {
        if (!binding.markdownInput.getInputText().isEmpty()) {
            this.showConfirmDialog();
        } else {
            super.finish();
        }

        // Might not actually finish, but it shouldn't matter
        overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
    }

    /**
     * Sends the reply
     *
     * @param view Ignored
     */
    public void sendReply(View view) {
        String text = binding.markdownInput.getInputText();

        // TODO add text change listener and disable button if empty
        if (text.isEmpty() || replyingTo == null) {
            return;
        }

        // Hide the keyboard
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }

        String id = replyingTo.getId();

        ReplyableRequest request = replyingTo instanceof RedditPost ? redditApi.post(id) : redditApi.comment(id);
        request.reply(text, comment -> {
            Log.d(TAG, "sendReply: Comment posted ");

            // No depth set means we're replying to a comment, set depth manually
            if (comment.getDepth() == -1) {
                comment.setDepth(((RedditComment)replyingTo).getDepth() + 1);
            }

            // Pass the new comment back and finish
            Intent intent = getIntent().putExtra(PostActivity.LISTING_KEY, new Gson().toJson(comment));

            // Kind of a bad way to do it, but if we call finish with text in the input a dialog is shown
            // Other option is to create a flag (ie "replySent") and not show the dialog if true
            binding.markdownInput.clearText();
            setResult(RESULT_OK, intent);
            finish();
        }, (error, t) -> {
            t.printStackTrace();
            Util.handleGenericResponseErrors(binding.parentLayout, error, t);
        });
    }


    /**
     * Shows a dialog to let the user confirm they want to leave
     *
     * <p>If the user selects to leave, {@link super#finish()} is called</p>
     */
    private void showConfirmDialog() {
        if (confirmDiscardDialog == null) {
            confirmDiscardDialog = new Dialog(this);
            confirmDiscardDialog.setContentView(R.layout.dialog_reply_confirm_back_press);
        }
        confirmDiscardDialog.show();

        // Because using match_parent in the layout file doesn't actually match the parent (screen width)
        // This looks weird on horizontal orientation though
        confirmDiscardDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Button discard = confirmDiscardDialog.findViewById(R.id.btnDiscard);
        Button cancel = confirmDiscardDialog.findViewById(R.id.btnCancel);

        discard.setOnClickListener(v -> super.finish());
        cancel.setOnClickListener(v -> confirmDiscardDialog.dismiss());

        // TODO add button for "discard and save" that saves the text and whats being responded to so we can resume later
    }

    /**
     * If there is no user logged in a dialog is shown to the user that
     * they are not logged in and won't be able to send a reply
     */
    private void showNotLoggedInDialogIfNotLoggedIn() {
        // TODO priate browsing
        if (!App.get().isUserLoggedIn()) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialogReplyNotLoggedInTitle))
                    .setMessage(getString(R.string.dialogReplyNotLoggedInContent))
                    .show();
        }
    }
}
