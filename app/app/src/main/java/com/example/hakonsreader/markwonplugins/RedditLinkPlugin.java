package com.example.hakonsreader.markwonplugins;

import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.DispatcherActivity;

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

    // TODO this should NOT match anything already in a markdown link (as people like to be funny with fake subreddit links)
    // Subreddits are alphanumericals, numbers, and underscores. Users are the same and dashes
    public static final Pattern RE = Pattern.compile(
            // Match whitespace, start of string, in a parenthesis, or not in a [] (ie. already a markdown link)
            "(^|\\s|\\(|(?=\\[))" +
            // Optional "/" at the start
            "/?" +
            "(" +
            // Subreddits: Match either r or R preceded by a / and characters, 0-9, _
            "([rR]/[A-Za-z09_]+)" +
            // Users: Match either u or U preceded by a / and characters, 0-9, _, -
            "|([uU]/[A-Za-z0-9_-]+)" +
            ")" +
            // Optional / at the end
            "/?");


    private Context context;

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
                s++;
            }

            final String link = textToSpan;
            final ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Intent intent = new Intent(context, DispatcherActivity.class);
                    intent.putExtra(DispatcherActivity.URL_KEY, link);

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
