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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.AbstractMarkwonPlugin;

/**
 * Plugin that wraps URLs in a clickable span redirecting to {@link DispatcherActivity} with
 * text formatting
 */
public class LinkPlugin extends AbstractMarkwonPlugin {
    private static final String TAG = "LinkPlugin";

    // TODO links with some characters dont get autolinked by markwon
    //  https://www.reddit.com/r/hakonschia/comments/jtyz0e/cool_image/ge534qc?utm_source=share&utm_medium=web2x&context=3

    // Pattern taken from: https://stackoverflow.com/a/3809435/7750841
    public static final Pattern RE = Pattern.compile(
            // Match whitespace, start of string, in a parenthesis, or not in a [] (ie. already a markdown link)
            "(^|\\s)" +
            "https://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=,{}\"\\s]*)"
    );


    private final Context context;

    public LinkPlugin(Context context) {
        this.context = context;
    }

    @Override
    public void beforeSetText(@NonNull TextView textView, @NonNull Spanned markdown) {
        final Spannable spannable = (Spannable) markdown;
        final String text = markdown.toString();
        final Matcher matcher = RE.matcher(text);

        while (matcher.find()) {
            final int s = matcher.start();
            final int e = matcher.end();
            final String link = matcher.group();
            Log.d(TAG, "beforeSetText: " + link);

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
