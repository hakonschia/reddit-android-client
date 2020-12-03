package com.example.hakonsreader.markwonplugins;

import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.DispatcherActivity;
import com.example.hakonsreader.api.utils.LinkUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.AbstractMarkwonPlugin;

/**
 * Markwon plugin to linkify Reddit links
 *
 * <p>Supported links are: r/subreddit, /r/subreddit, u/user and /u/user,</p>
 */
public class RedditLinkPlugin extends AbstractMarkwonPlugin {
    private static final String TAG = "RedditLinkPlugin";

    // Subreddits are alphanumericals, numbers, and underscores. Users are the same and dashes
    public static final Pattern RE = Pattern.compile(
            // Match whitespace, start of string, in a parenthesis, or not in a [] (ie. already a markdown link)
            "(^|\\s|\\(|(?=\\[))" +
            // Match either subreddit or user regex
            "(/?" + LinkUtils.BASE_SUBREDDIT_REGEX + ")" +
            "|(/?" + LinkUtils.BASE_USER_REGEX + ")"
    );


    private final Context context;

    /**
     * Creates a new reddit link plugin
     *
     * @param context The context to use for retrieving colors and starting activities
     */
    public RedditLinkPlugin(Context context) {
        this.context = context;
    }


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

            final String link = textToSpan;

            spannable.setSpan(new UnderlineSpan(), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.link_color)), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // By setting a URL span as well we can handle the link in InternalLinkMovementMethod which looks for
            // URLSpans, so we can get the highlight when touched
            spannable.setSpan(new URLSpan(link.trim()), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

}
