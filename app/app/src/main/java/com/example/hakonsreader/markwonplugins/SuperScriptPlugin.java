package com.example.hakonsreader.markwonplugins;

import android.text.Spannable;
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
    private static final String TAG = "RedditLinkPlugin";

    // TODO this doesn't allow for nesting, so nested superscripts dont work
    // Subreddits are alphanumericals, numbers, and underscores. Users are the same and dashes
    private static final Pattern RE = Pattern.compile("[\\^][(].*[)]");

    @Override
    public void beforeSetText(@NonNull TextView textView, @NonNull Spanned markdown) {
        applySuperSriptSpan((Spannable) markdown);
    }

    private void applySuperSriptSpan(@NonNull Spannable spannable) {
        final String text = spannable.toString();
        final Matcher matcher = RE.matcher(text);

        while (matcher.find()) {
            final int s = matcher.start();
            final int e = matcher.end();
            spannable.setSpan(new SuperscriptSpan(), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Hide the original syntax
            // The syntax is ^(...), so hide 2 characters at the start and one at the end
            spannable.setSpan(new HideSpoilerSyntaxSpan(), s, s + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new HideSpoilerSyntaxSpan(), e - 1, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}