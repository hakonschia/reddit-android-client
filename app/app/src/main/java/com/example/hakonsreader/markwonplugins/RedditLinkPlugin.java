package com.example.hakonsreader.markwonplugins;

import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Log;
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

    // Subreddits are alphanumericals, numbers, and underscores. Users are the same and dashes
    private static final Pattern RE = Pattern.compile("/?(([rR]/[A-Za-z09_]+)|([uU]/[A-Za-z0-9_-]+))/?");


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
        applySpoilerSpans((Spannable) markdown);
    }

    private void applySpoilerSpans(@NonNull Spannable spannable) {
        final String text = spannable.toString();
        final Matcher matcher = RE.matcher(text);

        while (matcher.find()) {
            final String textToSpan = matcher.group();
            Log.d(TAG, "applySpoilerSpans: " + textToSpan);

            final ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Intent intent = new Intent(context, DispatcherActivity.class);
                    intent.putExtra(DispatcherActivity.URL_KEY, textToSpan);

                    // The plugin is created from App, which is not an activity
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.linkColor = ContextCompat.getColor(context, R.color.linkColor);
                    ds.setColor(ds.linkColor);
                }
            };

            final int s = matcher.start();
            final int e = matcher.end();
            spannable.setSpan(clickableSpan, s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

}
