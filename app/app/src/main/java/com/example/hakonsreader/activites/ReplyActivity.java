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

        // TODO custom markwon plugins wont affect the edit text
        // Create editor
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
                // TODO If we're on a code block (4 spaces) and the new character is a newline, add 4 spaces automatically
                //  if we're in a list (starts with * or 1. (or technically <any digit>. ) then continue the list
                // Not implemented
            }
        });

        // TODO when the preview and edit text only takes up a limited amount of space this should scroll
        //  and also has the benefit of removing the link clicker which is kinda weird to have
        //binding.preview.setMovementMethod(ScrollingMovementMethod.getInstance());

        this.attachLongClickListenerToMarkdownButtons();
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
        }, (error, t) -> {
            t.printStackTrace();
            Util.handleGenericResponseErrors(binding.parentLayout, error, t);
        });
    }

    /**
     * Attaches a longClick listener to each of the markdown syntax buttons.
     *
     * <p>The listener looks for the "contentDescription" attribute and shows a toast with
     * the text if found</p>
     */
    private void attachLongClickListenerToMarkdownButtons() {
        View.OnLongClickListener markdownButtonsLongClick = v -> {
            CharSequence description = v.getContentDescription();

            if (description != null) {
                String desc = v.getContentDescription().toString();

                if (!desc.isEmpty()) {
                    Toast.makeText(ReplyActivity.this, description, Toast.LENGTH_SHORT).show();
                }
            }

            // Always return true, otherwise a long click with no description would act like
            // a single press, which is weird behaviour
            return true;
        };

        // Find all the children in the inner layout and add the listener
        int markdownButtonsCount = binding.markdownButtonsInnerLayout.getChildCount();
        for (int i = 0; i < markdownButtonsCount; i++) {
            View v = binding.markdownButtonsInnerLayout.getChildAt(i);
            v.setOnLongClickListener(markdownButtonsLongClick);
        }
    }

    /**
     * Adds the specified markdown syntax to an editable text. The cursor of the editable is moved
     * depending on if a selection of the text was highlighted, or moved to the middle of the syntax
     * so the user can start typing right away
     *
     * @param text The editable text to insert the syntax into
     * @param start The start of the text selection (where to insert the start syntax)
     * @param end The end of the text selection (where to insert the end syntax)
     * @param startSyntax The start of the markdown syntax, what comes before the text that defines
     *                   the start of this markdown (eg. {@code **} for bold)
     * @param endSyntax The end of the markdown syntax, what comes after the text that defines the closing
     *                  of this markdown (eg. {@code **} for bold)
     */
    public void addMarkdownSyntax(Editable text, int start, int end, String startSyntax, String endSyntax) {
        // Insert at the end first or else the end will be changed (or use end + startSyntax.length())
        text.insert(end, endSyntax);
        text.insert(start, startSyntax);

        // When no text is selected start == end
        // If we're not selecting a text to bold, set the cursor to the middle so we can start writing
        if (start == end) {
            // Set cursor to the middle
            binding.replyText.setSelection(start + startSyntax.length());
        }
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

        this.addMarkdownSyntax(binding.replyText.getText(), start, end, "**", "**");
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

        this.addMarkdownSyntax(binding.replyText.getText(), start, end, "*", "*");
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

        this.addMarkdownSyntax(binding.replyText.getText(), start, end, "~~", "~~");
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

            // We don't use addMarkdownSyntax here since we want to add the text and
            // link for the user automatically
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
        // Quote in markdown: >quote here

        // If we're on a line with a quote, remove it, if we're not, add at the start
        String text = binding.replyText.getText().toString();

        int start = binding.replyText.getSelectionStart();

        // Search backwards for a newline (start - 1 or else we might be on a newline if we're at the end of a line)
        int indexOfLine = text.lastIndexOf("\n", start - 1);

        int startPos = indexOfLine;

        // No newline found means it's the first line, set pos to the start of the text
        if (indexOfLine == -1) {
            startPos = 0;
        } else {
            // If we're not on the first line we need to add one as we want to start after the newline
            // not on it (the newline is on the end of the previous line, not the first character of the new line)
            startPos++;
        }

        // Already a quote, remove it (if text is empty we're guaranteed to not have a >)
        if (!text.isEmpty() && text.charAt(startPos) == '>') {
            // Remove ">"
            binding.replyText.getText().delete(startPos, startPos + 1);
        } else {
            binding.replyText.getText().insert(startPos, ">");
        }

        Log.d(TAG, "quoteOnClick: indexOfLine=" + indexOfLine);
    }

    /**
     * onClick for the "Spoiler" button. Adds spoiler markdown to the text where the cursor is.
     *
     * <p>Note: The spoiler syntax is Reddit specific and not part of the official markdown specification</p>
     *
     * @param view Ignored
     */
    public void spoilerOnClick(View view) {
        // Reddit spoiler markdown syntax: >!Spoiler goes here!<

        int start = binding.replyText.getSelectionStart();
        int end = binding.replyText.getSelectionEnd();

        this.addMarkdownSyntax(binding.replyText.getText(), start, end, ">!", "!<");
    }

    /**
     * onClick for the "Superscript" button. Adds superscript markdown to the text where the cursor is.
     *
     * @param view Ignored
     */
    public void superscriptOnClick(View view) {
        // Superscript in markdown: ^(text goes here)
        // It is also possible to use a single "^" for single words, but it's safer (and easier) to always add the full thing

        int start = binding.replyText.getSelectionStart();
        int end = binding.replyText.getSelectionEnd();

        this.addMarkdownSyntax(binding.replyText.getText(), start, end, "^(", ")");
    }

    /**
     * onClick for the "Inline code" button. Adds inline code markdown to the text where the cursor is.
     *
     * <p>For code blocks use {@link ReplyActivity}</p>
     *
     * @param view Ignored
     */
    public void inlineCodeOnClick(View view) {
        // Code in markdown: `code goes here`
        // Code in backticks are inlined, which means they can appear in the text (as opposed to code blocks
        // which are multiline)

        int start = binding.replyText.getSelectionStart();
        int end = binding.replyText.getSelectionEnd();

        this.addMarkdownSyntax(binding.replyText.getText(), start, end, "`", "`");
    }

    /**
     * onClick for the "Code block" button. Adds code block markdown to the text where the cursor is.
     *
     * <p>For code blocks use {@link ReplyActivity}</p>
     *
     * @param view Ignored
     */
    public void codeBlockOnClick(View view) {
        // Code blocks in markdown: "    code goes here" (four spaces at the start)

        int start = binding.replyText.getSelectionStart();
        int end = binding.replyText.getSelectionEnd();

        // Add two newlines at the start, since one newline in markdown usually does nothing (but after
        // code blocks they do, so at the end we can add only one)
        // There isn't really an end for code blocks. They end when a new line doesn't have 4 spaces
        this.addMarkdownSyntax(binding.replyText.getText(), start, end, "\n\n    ", "\n");
    }

    /**
     * onClick for the "Bullet list" button. Adds bullet list markdown to the text where the cursor is.
     *
     * <p>For code blocks use {@link ReplyActivity}</p>
     *
     * @param view Ignored
     */
    public void bulletListOnClick(View view) {
        // Bullet list in markdown: Each list item starts with "* "

        int start = binding.replyText.getSelectionStart();
        int end = binding.replyText.getSelectionEnd();

        // Add two newlines at the start, since one newline in markdown usually does nothing (but after
        // code blocks they do, so at the end we can add only one)
        // There isn't really an end for code blocks. They end when a new line doesn't have 4 spaces
        this.addMarkdownSyntax(binding.replyText.getText(), start, end, "* ", "");
    }

    /**
     * onClick for the "Numbered list" button. Adds numbered list markdown to the text where the cursor is.
     *
     * <p>For code blocks use {@link ReplyActivity}</p>
     *
     * @param view Ignored
     */
    public void numberedListOnClick(View view) {
        // Bullet list in markdown: Each list item starts with "1. ", it doesn't matter which number in the
        // list we are, having 1. at the start works for each and increments it correctly.
        // Markwon allows to start at other numbers and increments from there, but Reddit doesn't recognize that
        // Ie. first item is "3. " then the next item will always be 4, then 5 etc.

        int start = binding.replyText.getSelectionStart();
        int end = binding.replyText.getSelectionEnd();

        this.addMarkdownSyntax(binding.replyText.getText(), start, end, "1. ", "");
    }
}
