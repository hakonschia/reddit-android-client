package com.example.hakonsreader.markwonplugins;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.SuperscriptSpan;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.AbstractMarkwonPlugin;

/**
 * Markwon plugin for Reddit superscript
 */
public class SuperScriptPlugin extends AbstractMarkwonPlugin {
    private static final String TAG = "SuperScriptPlugin";

    // TODO this doesn't allow for nesting, so nested superscripts dont work
    // Subreddits are alphanumericals, numbers, and underscores. Users are the same and dashes
    public static final Pattern RE = Pattern.compile("[\\^][(].*[)]");

    @Override
    public void beforeSetText(@NonNull TextView textView, @NonNull Spanned markdown) {
        applySuperSriptSpan((Spannable) markdown, 0, markdown.length());
    }

    private void applySuperSriptSpan(@NonNull Spannable spannable, int start, int end) {
        final String original = spannable.toString();
        final String text = original.substring(start, end);
        final Matcher matcher = RE.matcher(text);

        while (matcher.find()) {
            final int s = matcher.start();
            final int e = matcher.end();

            // Apply the superspan recursively as a superspan can have a superspan inside of it
            // Ie. ^(hello ^(there))
            applySuperSriptSpan(spannable, s + 2, e - 1);

            int spanStart = s + start;
            int spanEnd = e + start;

            spannable.setSpan(new SuperscriptSpan(), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Hide the original syntax
            // The syntax is ^(...), so hide 2 characters at the start and one at the end
            spannable.setSpan(new HideSyntaxSpan(), spanStart, spanStart + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new HideSyntaxSpan(), spanEnd - 1, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}