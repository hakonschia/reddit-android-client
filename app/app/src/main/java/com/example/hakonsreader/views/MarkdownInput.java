package com.example.hakonsreader.views;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.databinding.MarkdownInputBinding;

import io.noties.markwon.Markwon;
import io.noties.markwon.editor.MarkwonEditor;
import io.noties.markwon.editor.MarkwonEditorTextWatcher;

/**
 * Class wrapping a text input field with markdown buttons to easily insert markdown formatting
 * into the input field
 */
public class MarkdownInput extends FrameLayout {
    private static final String TAG = "MarkdownInput";


    private final MarkdownInputBinding binding;

    /**
     * Dialog that allows the user to easily insert a markdown link. If this is
     * not null a dialog is shown to the user
     */
    private Dialog linkDialog;


    public MarkdownInput(Context context) {
        this(context, null, 0, 0);
    }
    public MarkdownInput(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }
    public MarkdownInput(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }
    public MarkdownInput(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        binding = MarkdownInputBinding.inflate(LayoutInflater.from(context), this, true);
        this.setup();
    }

    private void setup() {
        this.bindMarkdownClickListeners();
        this.bindMarkdownLongClickListeners();
        this.setTextListeners();
    }

    /**
     * Binds the markdown button click listeners to its appropriate function
     */
    private void bindMarkdownClickListeners() {
        // Setting onClick from XML doesn't work since it doesn't find the functions not in an activity
        binding.markdownBold.setOnClickListener(this::boldOnClick);
        binding.markdownItalic.setOnClickListener(this::italicOnClick);
        binding.markdownStrikethrough.setOnClickListener(this::strikethroughOnClick);
        binding.markdownLink.setOnClickListener(this::linkOnClick);
        binding.markdownQuote.setOnClickListener(this::quoteOnClick);
        binding.markdownSpoiler.setOnClickListener(this::spoilerOnClick);
        binding.markdownSuperscript.setOnClickListener(this::superscriptOnClick);
        binding.markdownInlineCode.setOnClickListener(this::inlineCodeOnClick);
        binding.markdownCodeBlock.setOnClickListener(this::codeBlockOnClick);
        binding.markdownBulletList.setOnClickListener(this::bulletListOnClick);
        binding.markdownNumberedList.setOnClickListener(this::numberedListOnClick);
    }

    /**
     * Sets various text listeners on {@link MarkdownInputBinding#replyText} to update the
     * text to highlight markdown syntax, to automatically continue markdown syntax, and to update the preview text
     */
    private void setTextListeners() {
        // With a MarkwonEditor the text input shows some highlighting for what is markdown

        // Using this markwon instance has the benefit of highlighting reddit links etc. in the preview
        // Although it doesn't actually add the links around the markdown, reddit doesn't do that
        // themselves so it's fine
        final Markwon markwon = App.Companion.get().getMarkwon();

        // TODO custom markwon plugins wont affect the edit text
        // Create editor
        final MarkwonEditor editor = MarkwonEditor.create(markwon);

        // Set text listeners
        // Updates the EditText to show what is markdown syntax
        binding.replyText.addTextChangedListener(MarkwonEditorTextWatcher.withProcess(editor));

        // Listens to changes and automatically continues markdown syntax
        binding.replyText.addTextChangedListener(new MarkdownInsertTextWatcher());
    }

    /**
     * Attaches a longClick listener to each of the markdown syntax buttons.
     *
     * <p>The listener looks for the "contentDescription" attribute and shows a toast with
     * the text if found</p>
     */
    private void bindMarkdownLongClickListeners() {
        View.OnLongClickListener markdownButtonsLongClick = v -> {
            CharSequence description = v.getContentDescription();

            if (description != null) {
                String desc = v.getContentDescription().toString();

                if (!desc.isEmpty()) {
                    Toast.makeText(getContext(), description, Toast.LENGTH_SHORT).show();
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
     * Adds a {@link TextWatcher} to the input field
     *
     * @param watcher The watched to add
     */
    public void addTextChangeListener(TextWatcher watcher) {
        binding.replyText.addTextChangedListener(watcher);
    }

    /**
     * Clears the text in the input field
     */
    public void clearText() {
        binding.replyText.setText("");
    }

    /**
     * Sets the text to show in the input field
     *
     * @param text The text to show
     */
    public void setText(String text) {
        binding.replyText.setText(text);
    }


    /**
     * Retrieves the current text in the input field
     *
     * @return The Markdown in the input field
     */
    public String getInputText() {
        return binding.replyText.getText().toString();
    }

    /**
     * Shows a preview of the markdown input in a popup dialog
     */
    public void showPreviewInPopupDialog() {
        // An AlertDialog didn't render custom markwon plugins, so use a custom dialog
        Dialog previewDialog = new Dialog(getContext());
        previewDialog.setContentView(R.layout.dialog_markdown_preview);
        TextView previewText = previewDialog.findViewById(R.id.previewText);
        App.Companion.get().getMarkwon().setMarkdown(previewText, binding.replyText.getText().toString());

        previewDialog.show();
        previewDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

        // TODO if a text is marked we can probably pass this here, but the question is if it makes sense
        //  to put it as the link or the link text (probably as link?)
        //  We probably need to remove the text selected as well if we add the link, otherwise the markdown link
        //  would  be added as well as the previous text
        this.showLinkDialog("", "");
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
        // If startPos was incremented to the text length (we're at an empty line at the end of the text)
        // look at the previous character as it would otherwise cause a crash
        if (!text.isEmpty() && text.charAt(startPos == text.length() ? startPos - 1 : startPos) == '>') {
            // Remove ">"
            binding.replyText.getText().delete(startPos, startPos + 1);
        } else {
            binding.replyText.getText().insert(startPos, ">");
        }
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
     * @param view Ignored
     */
    public void codeBlockOnClick(View view) {
        // Code blocks in markdown: "    code goes here" (four spaces at the start)
        // Technically markdown also supports "```code here```" which also supports multiline, but apparently
        // Reddit doesn't support this on every client, so we always use four spaces

        int start = binding.replyText.getSelectionStart();
        int end = binding.replyText.getSelectionEnd();
        String text = binding.replyText.getText().toString();

        String startSyntax = "";

        // TODO this should add the remaining newlines needed (ie. if one is already there only add one)
        // Ensure there are two newlines before the code block, unless we're at the beginning of the text
        // or we already have two newlines (one newline in markdown usually doesn't do anything, so we need two)
        if (!(start == 0 || (text.length() >= 2 && text.startsWith("\n\n", start - 2)))) {
            startSyntax = "\n\n";
        }

        // Add the four spaces which is the actual code block syntax
        startSyntax += "    ";

        // There isn't really an end for code blocks. They end when a new line doesn't have 4 spaces
        this.addMarkdownSyntax(binding.replyText.getText(), start, end, startSyntax, "");
    }

    /**
     * onClick for the "Bullet list" button. Adds bullet list markdown to the text where the cursor is.
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

    /**
     * Shows a dialog to let the user insert a link into the markdown text
     *
     * @param linkText The initial text to set for the link text
     * @param link The initial text to set for the link
     */
    public void showLinkDialog(String linkText, String link) {
        if (linkDialog == null) {
            linkDialog = new Dialog(getContext());
            linkDialog.setContentView(R.layout.dialog_reply_add_link);
        }

        linkDialog.show();

        // Because using match_parent in the layout file doesn't actually match the parent (screen width)
        // This looks weird on horizontal orientation though
        linkDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView textTv = linkDialog.findViewById(R.id.textText);
        TextView linkTv = linkDialog.findViewById(R.id.linkText);

        Button add = linkDialog.findViewById(R.id.btnAddLink);
        Button cancel = linkDialog.findViewById(R.id.btnCancelLink);

        add.setOnClickListener(v -> {
            String textToAdd = textTv.getText().toString();
            String linkToAdd = linkTv.getText().toString();
            linkToAdd = linkToAdd.replace(" ", "%20");

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
                add.setEnabled(!textTv.getText().toString().isEmpty() && !linkTv.getText().toString().isEmpty());
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
        textTv.addTextChangedListener(enableAddButtonTextWatcher);
        linkTv.addTextChangedListener(enableAddButtonTextWatcher);

        // If there's initial text to set it has to be set after the listener or else the listener
        // won't trigger
        textTv.setText(linkText == null ? "" : linkText);
        linkTv.setText(link == null ? "" : link);
    }

    /**
     * @return True if the link dialog is currently shown to the user
     */
    public boolean isLinkDialogShown() {
        return linkDialog != null && linkDialog.isShowing();
    }

    /**
     * Dismisses the link dialog if it is shown
     */
    public void dismissLinkDialog() {
        if (isLinkDialogShown()) {
            linkDialog.dismiss();
        }
    }

    /**
     * If the link dialog is shown this gets the text input into the link dialog (ie. what the link
     * is shown as when hyperlinked)
     *
     * @return The text of the link dialog. If the dialog isn't shown this returns an empty string
     * @see MarkdownInput#getLinkDialogLink()
     * @see MarkdownInput#showLinkDialog(String, String)
     */
    public String getLinkDialogText() {
        if (!isLinkDialogShown()) {
            return "";
        }

        TextView text = linkDialog.findViewById(R.id.textText);
        return text.getText().toString();
    }

    /**
     * If the link dialog is shown this gets the link input into the link dialog (ie. what the link
     * is linking to when hyperlinked)
     *
     * @return The link of the link dialog. If the dialog isn't shown this returns an empty string
     * @see MarkdownInput#getLinkDialogText()
     * @see MarkdownInput#showLinkDialog(String, String)
     */
    public String getLinkDialogLink() {
        if (!isLinkDialogShown()) {
            return "";
        }

        TextView text = linkDialog.findViewById(R.id.linkText);
        return text.getText().toString();
    }


    /**
     * Class that implements {@link TextWatcher} that inserts automatically continues various
     * markdown formatting.
     *
     * <p>Current markdown that is added:
     * <ol>
     *     <li>Code block</li>
     * </ol>
     * </p>
     */
    private static class MarkdownInsertTextWatcher implements TextWatcher {
        /**
         * When this is set to true the next call to onTextChanged is ignored
         */
        private boolean textModified = false;

        /**
         * When this is true the next call to afterTextChanged inserts code block formatting
         * at the position specified by {@link MarkdownInsertTextWatcher#posToInsert}
         */
        private boolean insertCodeBlock = false;

        /**
         * The position in the editable text in afterTextChanged to insert formatting
         */
        private int posToInsert = -1;


        /**
         * {@inheritDoc}
         */
        @Override
        public void afterTextChanged(Editable s) {
            if (insertCodeBlock) {
                // Set that the text has been modified, so the next call to onTextChanged is ignored
                textModified = true;

                // Ensure this is reset
                insertCodeBlock = false;

                // The text has to be inserted as the last thing that happens, otherwise it will cause an infinite loop
                s.insert(posToInsert, "    ");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // TODO If we're on a code block (4 spaces) and the new character is a newline, add 4 spaces automatically
            //  if we're in a list (starts with * or 1. (or technically <any digit>. ) then continue the list

            // If the text has been modified in afterTextChanged don't do anything as it can cause
            // an infinite loop
            if (textModified) {
                textModified = false;
                return;
            }

            String text = s.toString();
            int length = text.length();

            // This seems to be correct?
            // This seemingly gives the same value as "textView.getSelectionStart()"
            int pos = start + count;

            // So this doesn't really work as intended, since if we're removing a newline instead of adding it
            // this will still trigger and can insert a code block/list at the start without being able to remove
            // the newline
            // TODO We really need to figure out how to check that the character inserted was a newline, not just
            //  that the previous position is a newline
            if (length > 0 && pos > 0 && text.charAt(pos - 1) == '\n') {

                // Start at pos - 2 as we don't want to count the newly inserted newline
                int lastNewline = text.lastIndexOf('\n', pos - 2);

                // If we're at the first line, set to 0 to count the start of the line
                if (lastNewline == -1) {
                    lastNewline = 0;
                } else {
                    // If we're not on the first line, go one further as the newline is
                    // actually on the end of the previous line
                    lastNewline++;
                }

                // Find the next newline to find the entire line
                int nextNewLine = text.indexOf('\n', pos);
                if (nextNewLine == -1) {
                    nextNewLine = text.length();
                }

                // substring(begin, end) is end exclusive, so the newline at the end isn't counted
                String line = text.substring(lastNewline, nextNewLine);

                // If the line starts with a code block, but isn't just the syntax (and a newline)
                // This makes it so if the user presses enter and it inserts a new code block
                // and press enter again it doesn't continue the code block, but continues as normal text
                if (line.startsWith("    ") && !line.equals("    \n")) {
                    // The pos is on the new line, so that's where the code block syntax should be inserted
                    posToInsert = pos;
                    insertCodeBlock = true;
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Not implemented
            // can probably find out here if a newline is added?
        }
    }
}
