package com.example.hakonsreader.markwonplugins;

import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
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
            "(" + LinkUtils.SUBREDDIT_REGEX_NO_HTTPS + ")" +
            "|(" + LinkUtils.USER_REGEX + ")"
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
            Log.d(TAG, "applyLinkSpanRedditLinkPlugin: " + textToSpan);

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
            final ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Intent intent = new Intent(context, DispatcherActivity.class);
                    intent.putExtra(DispatcherActivity.URL_KEY, link.trim());

                    // The plugin is created from App, which is not an activity
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.linkColor = ContextCompat.getColor(context, R.color.link_color);
                    ds.setColor(ds.linkColor);
                }
            };

            spannable.setSpan(clickableSpan, s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new UnderlineSpan(), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

}
