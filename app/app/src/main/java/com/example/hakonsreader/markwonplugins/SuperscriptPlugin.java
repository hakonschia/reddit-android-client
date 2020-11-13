package com.example.hakonsreader.markwonplugins;

import android.text.Spannable;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.AbstractMarkwonPlugin;

/**
 * Markwon plugin for Reddit superscript
 */
public class SuperscriptPlugin extends AbstractMarkwonPlugin {
    private static final String TAG = "SuperScriptPlugin";

    // Superscript syntax is either ^, for one word superscripts, or ^() to for sentences with spaces

    // The syntax is ^(), so match starting with ^(, and any character not ) and then a )
    // We need to match anything not a ) inside to avoid multiple in a row being treated as one
    // ie. ^(hello) ^(there)
    public static final String RE_SENTENCES = "\\^\\([^)]+\\)";

    // For words: match ^ and then any character not a whitespace at least once
    public static final String RE_WORDS = "\\^[^\\s]+";

    // Combine both patterns with an OR operator
    public static final Pattern RE = Pattern.compile(String.format("(%s)|(%s)", RE_SENTENCES, RE_WORDS));

    @Override
    public void beforeSetText(@NonNull TextView textView, @NonNull Spanned markdown) {
        applySuperscript((Spannable) markdown, 0, markdown.length());
    }

    /**
     * Applies superscript to the spannable
     *
     * @param spannable The spannable to apply to
     * @param start Where in the spannable to start
     * @param end Where in the spannable to end
     */
    private void applySuperscript(@NonNull Spannable spannable, int start, int end) {
        final String text = spannable.toString().substring(start, end);
        final Matcher matcher = RE.matcher(text);

        while (matcher.find()) {
            final int s = matcher.start();
            final int e = matcher.end();
            final String g = matcher.group();

            int spanStart = s + start;
            int spanEnd = e + start;

            // SuperscriptSpan puts it higher, and RelativeSizeSpan makes the text smaller
            spannable.setSpan(new SuperscriptSpan(), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new RelativeSizeSpan(0.9f), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Assume we are in a word superscript: ^word
            int startSyntaxLength = 1;

            // Sentence superscript: ^(sentence here). The start syntax is 2 characters, and also has syntax at the end to hide
            if (g.startsWith("^(")) {
                startSyntaxLength = 2;

                // This is super hacky and I don't even know why this works, but for nested superscripts it doesn't match completely
                // and it would not hide the last ) unless we go to spanEnd + 1, but if we are at the end we obviously can't
                // go past the end
                if (end == spannable.length()) {
                    spannable.setSpan(new RelativeSizeSpan(0f), spanEnd - 1, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    spannable.setSpan(new RelativeSizeSpan(0f), spanEnd - 1, spanEnd + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }

            // Hide the original syntax. Using RelativeSizeSpan with 0 as proportion effectively removes the text
            // The syntax is ^(...), so hide 2 characters at the start and one at the end
            spannable.setSpan(new RelativeSizeSpan(0f), spanStart, spanStart + startSyntaxLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Apply the superspan recursively as a superspan can have a superspan inside of it
            // Ie. ^(hello ^(there))
            applySuperscript(spannable, s + startSyntaxLength, e);
        }
    }
}