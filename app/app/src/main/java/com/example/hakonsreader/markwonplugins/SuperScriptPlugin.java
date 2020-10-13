package com.example.hakonsreader.markwonplugins;

import android.text.Spannable;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
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

    // TODO apparently superscripts are either ^() or ^

    // The syntax is ^(), so match starting with ^(, and any character not ) and then a )
    // We need to match anything not a ) inside to avoid multiple in a row being treated as one
    // ie. ^(hello) ^(there) but it ruins recursive superscripts arghhhh
    public static final Pattern RE = Pattern.compile("\\^\\([^)]+\\)");

    @Override
    public void beforeSetText(@NonNull TextView textView, @NonNull Spanned markdown) {
        applySuperSriptSpan((Spannable) markdown, 0, markdown.length());
    }

    private void applySuperSriptSpan(@NonNull Spannable spannable, int start, int end) {
        final String text = spannable.toString().substring(start, end);
        final Matcher matcher = RE.matcher(text);

        while (matcher.find()) {
            final int s = matcher.start();
            final int e = matcher.end();

            // Apply the superspan recursively as a superspan can have a superspan inside of it
            // Ie. ^(hello ^(there))
            applySuperSriptSpan(spannable, s + 2, e);

            int spanStart = s + start;
            int spanEnd = e + start;

            // SuperscriptSpan puts it higher, and RelativeSizeSpan makes the text smaller
            spannable.setSpan(new SuperscriptSpan(), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new RelativeSizeSpan(0.85f), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);


            // Hide the original syntax
            // The syntax is ^(...), so hide 2 characters at the start and one at the end
            spannable.setSpan(new HideSyntaxSpan(), spanStart, spanStart + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // This is super hacky and I don't even know why this works, but for nested superscripts it doesn't match completely
            // and it would not hide the last ) unless we go to spanEnd + 1, but if we are at the end we obviously can't
            // go past the end
            if (end == spannable.length()) {
                spannable.setSpan(new HideSyntaxSpan(), spanEnd - 1, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                spannable.setSpan(new HideSyntaxSpan(), spanEnd - 1, spanEnd + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

        }
    }
}