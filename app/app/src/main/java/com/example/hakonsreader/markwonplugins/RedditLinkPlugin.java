package com.example.hakonsreader.markwonplugins;

import android.text.Spannable;
import android.text.Spanned;
import android.text.style.URLSpan;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.AbstractMarkwonPlugin;

/**
 * Markwon plugin to linkify Reddit links. This plugin wraps the text in a {@link URLSpan}
 *
 * <p>Supported links are: r/subreddit and /r/subreddit, u/user and /u/user, user/user and /user/user</p>
 */
public class RedditLinkPlugin extends AbstractMarkwonPlugin {
    private static final String TAG = "RedditLinkPlugin";

    // Subreddits are alphanumericals, numbers, and underscores. Users are the same and dashes
    public static final Pattern RE = Pattern.compile(
            // Match whitespace, start of string, in a parenthesis, or not in a [] (ie. already a markdown link)
            "(^|\\s|\\(|(?=\\[))" +
            // Match either subreddit or user regex
            // Slash at the beginning and end is optional
            "/?((r/[a-z0-9_]+)|(u(ser)?/[a-z0-9_-]+))/?"
    , Pattern.CASE_INSENSITIVE);


    @Override
    public void beforeSetText(@NonNull TextView textView, @NonNull Spanned markdown) {
        applyLinkSpan((Spannable) markdown);
    }

    private void applyLinkSpan(@NonNull Spannable spannable) {
        final String text = spannable.toString();
        final Matcher matcher = RE.matcher(text);

        while (matcher.find()) {
            String textToSpan = matcher.group();
            int s = matcher.start();
            int e = matcher.end();

            // The link is inside parenthesis such as a superscript, remove the parenthesis
            if (textToSpan.startsWith("(")) {
                textToSpan = textToSpan.substring(1);
            }

            // If the text starts with a space make sure the space isn't linked, ie. "a r/subreddit"
            // would cause " r/subreddit" to be linked, which both looks weird and causes issues in
            // DispatcherActivity with /r/ prefixes
            if (textToSpan.startsWith(" ")) {
                textToSpan = textToSpan.substring(1);
                s++;
            }

            final String link = textToSpan.trim();

            // By setting a URL span as well we can handle the link in InternalLinkMovementMethod which looks for
            // URLSpans, so we can get the highlight when touched
            spannable.setSpan(new URLSpan(link), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

}
