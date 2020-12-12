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
 * Plugin that wraps raw URLs in a {@link URLSpan}
 */
public class LinkPlugin extends AbstractMarkwonPlugin {
    private static final String TAG = "LinkPlugin";

    // Pattern taken from: https://stackoverflow.com/a/3809435/7750841
    private static final Pattern RE = Pattern.compile("https://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=,]*)");


    @Override
    public void beforeSetText(@NonNull TextView textView, @NonNull Spanned markdown) {
        final Spannable spannable = (Spannable) markdown;
        final String text = markdown.toString();
        final Matcher matcher = RE.matcher(text);

        while (matcher.find()) {
            final int s = matcher.start();
            final int e = matcher.end();
            final String link = matcher.group();

            spannable.setSpan(new URLSpan(link), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
