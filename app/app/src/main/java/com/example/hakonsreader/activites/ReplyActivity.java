package com.example.hakonsreader.activites;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;

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
import com.example.hakonsreader.misc.Util;
import com.google.gson.Gson;

import io.noties.markwon.Markwon;
import io.noties.markwon.editor.MarkwonEditor;
import io.noties.markwon.editor.MarkwonEditorTextWatcher;

public class ReplyActivity extends AppCompatActivity {
    private static final String TAG = "ReplyActivity";

    private static final String REPLY_TEXT = "replyText";
    private static final String PREVIEW_TEXT = "previewText";


    private ActivityReplyBinding binding;

    private final RedditApi redditApi = App.get().getApi();
    private RedditListing replyingTo;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReplyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null) {
            binding.replyText.setText(savedInstanceState.getString(REPLY_TEXT));
            binding.preview.setText(savedInstanceState.getString(PREVIEW_TEXT));
        }

        // TODO get comment/post
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

        // With adding a MarkwonEditor the text input shows some highlighting for what is markdown

        // Using this markwon instance has the benefit of highlighting reddit links etc. in the preview
        // Although it doesn't actually add the links around the markdown, reddit doesn't do that
        // themselves so it's fine
        final Markwon markwon = App.get().getMark();

        // create editor
        final MarkwonEditor editor = MarkwonEditor.create(markwon);

        // Set edit listeners. Both the edit text field and and preview are updated
        binding.replyText.addTextChangedListener(MarkwonEditorTextWatcher.withProcess(editor));
        binding.replyText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // It's probably horribly inefficient to update the entire markdown every text change
                // Might be possible to implement onTextChanged and render only the markdown that changed
                // and insert it into the text. This works for now
                markwon.setMarkdown(binding.preview, s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not implemented
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not implemented
            }
        });

        // TODO when the preview and edit text only takes up a limited amount of space this should scroll
        //  and also has the benefit of removing the link clicker which is kinda weird to have
        //binding.preview.setMovementMethod(ScrollingMovementMethod.getInstance());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Store what is in the edit text
        outState.putString(REPLY_TEXT, binding.replyText.getText().toString());
        outState.putString(PREVIEW_TEXT, binding.preview.getText().toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
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

        // Hide the keyboard
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }

        String id = replyingTo.getId();

        ReplyableRequest request = replyingTo instanceof RedditPost ? redditApi.post(id) : redditApi.comment(id);
        request.reply(text, comment -> {
            Log.d(TAG, "sendReply: Comment posted");

            // TODO for replies to comments the depth has to be set manually

            // Pass the new comment back and finish
            Intent intent = getIntent().putExtra(PostActivity.LISTING_KEY, new Gson().toJson(comment));

            setResult(RESULT_OK, intent);
            finish();
        }, (code, t) -> {
            t.printStackTrace();
            Util.handleGenericResponseErrors(binding.parentLayout, code, t);
        });
    }


    /**
     * onClick for the "Bold" button. Adds bold markdown to the text where the cursor is
     *
     * @param view Ignored
     */
    public void boldOnClick(View view) {
        // Bold text in markdown: **bold text here**

        int start = binding.replyText.getSelectionStart();
        int end = binding.replyText.getSelectionEnd();

        // Insert at the end first or else the end will be changed (or use end + 2)
        binding.replyText.getText().insert(end, "**");
        binding.replyText.getText().insert(start, "**");

        // When no text is selected start == end
        // If we're not selecting a text to bold, set the cursor to the middle so we can start writing
        if (start == end) {
            // Set cursor to the middle
            binding.replyText.setSelection(start + 2);
        }
    }

    /**
     * onClick for the "Italic" button. Adds italic markdown to the text where the cursor is
     *
     * @param view Ignored
     */
    public void italicOnClick(View view) {
        // Italic text in markdown: *italic text here*
        int start = binding.replyText.getSelectionStart();
        int end = binding.replyText.getSelectionEnd();

        // Insert at the end first or else the end will be changed (or use end + 1)
        binding.replyText.getText().insert(end, "*");
        binding.replyText.getText().insert(start, "*");

        // When no text is selected start == end
        // If we're not selecting a text to bold, set the cursor to the middle so we can start writing
        if (start == end) {
            // Set cursor to the middle
            binding.replyText.setSelection(start + 1);
        }
    }

    /**
     * onClick for the "Strikethrough" button. Adds strikethrough markdown to the text where the cursor is
     *
     * @param view Ignored
     */
    public void strikethroughOnClick(View view) {
        // Strikethrough in markdown: ~~striked text here~~

        int start = binding.replyText.getSelectionStart();
        int end = binding.replyText.getSelectionEnd();

        // Insert at the end first or else the end will be changed (or use end + 2)
        binding.replyText.getText().insert(end, "~~");
        binding.replyText.getText().insert(start, "~~");

        // When no text is selected start == end
        // If we're not selecting a text to bold, set the cursor to the middle so we can start writing
        if (start == end) {
            // Set cursor to the middle
            binding.replyText.setSelection(start + 2);
        }
    }

    /**
     * onClick for the "Link" button. Opens a dialog to allow the user to insert a link with markdown
     * and adds the markdown to the text
     *
     * @param view Ignored
     */
    public void linkOnClick(View view) {
        // Link in markdown: [Link text](https://link.com)

        // Create the dialog and open it
        Dialog linkDialog = new Dialog(this);
        linkDialog.setContentView(R.layout.reply_add_link_dialog);
        linkDialog.setOnDismissListener(dialog -> {
            Log.d(TAG, "linkOnClick: dismissed");
        });
        linkDialog.show();

        TextView text = linkDialog.findViewById(R.id.textText);
        TextView link = linkDialog.findViewById(R.id.linkText);

        Button add = linkDialog.findViewById(R.id.btnAddLink);
        Button cancel = linkDialog.findViewById(R.id.btnCancelLink);


        add.setOnClickListener(v -> {
            String textToAdd = text.getText().toString();
            String linkToAdd = link.getText().toString();

            int start = binding.replyText.getSelectionStart();
            int end = binding.replyText.getSelectionEnd();

            Editable editText = binding.replyText.getText();

            // Insert the end first so we don't have to care about the length of the text and offset the end
            editText.insert(end, String.format("(%s)", linkToAdd));
            editText.insert(start, String.format("[%s]", textToAdd));
            
            linkDialog.dismiss();
        });

        cancel.setOnClickListener(v -> {
            // TODO if text or link aren't empty ask "Do you want to dismiss" (which is weird since it
            //  would have to open another dialog?
            linkDialog.dismiss();
        });

        // Text listener that enables/disables the add button if either the text or link is empty
        TextWatcher enableAddButtonTextWatcher = new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // TODO it's pretty hard to see if the button is enabled or disabled
                add.setEnabled(!text.getText().toString().isEmpty() && !link.getText().toString().isEmpty());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not implemented
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not implemented
            }
        };

        // Add the listener to both the text and the link
        text.addTextChangedListener(enableAddButtonTextWatcher);
        link.addTextChangedListener(enableAddButtonTextWatcher);
    }

    /**
     * onClick for the "Quote" button. If the line the cursor is on has quote markdown already,
     * it is remove. If there is no quote markdown on the start of the line, it is added
     *
     * @param view Ignored
     */
    public void quoteOnClick(View view) {
        // Quote in markdown: > quote here

        // If we're on a line with a quote, remove it, if we're not, add at the start
        String text = binding.replyText.getText().toString();

        if (text.isEmpty()) {
            return;
        }
    }
}
