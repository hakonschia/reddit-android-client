package com.example.hakonsreader.markwonplugins;

import android.text.Spannable;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.SuperscriptSpan;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.squareup.picasso.Picasso;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.AbstractMarkwonPlugin;

/**
 * Markwon plugin for Reddit superscript
 */
public class SuperScriptPlugin extends AbstractMarkwonPlugin {
    private static final String TAG = "SuperScriptPlugin";

    // Superscript syntax is either ^, for one word superscripts, or ^() to for sentences with spaces

    // The syntax is ^(), so match starting with ^(, and any character not ) and then a )
    // We need to match anything not a ) inside to avoid multiple in a row being treated as one
    // ie. ^(hello) ^(there)
    // TODO this should allow for ^(hel^(lo)) (it currently only works if there is a space between 'l' and '^')
    public static final Pattern RE = Pattern.compile("\\^\\([^)]+\\)");

    // For words: match ^ and then any character not a whitespace as that is the end of the word
    public static final Pattern RE2 = Pattern.compile("\\^[^\\s]+");

    @Override
    public void beforeSetText(@NonNull TextView textView, @NonNull Spanned markdown) {
       // applySuperSriptSpan((Spannable) markdown, 0, markdown.length());
        applySuperSriptSpan2((Spannable) markdown);
    }

    private void applySuperSriptSpan(@NonNull Spannable spannable, int start, int end) {
        final String text = spannable.toString().substring(start, end);
        final Matcher matcher = RE.matcher(text);

        while (matcher.find()) {
            final int s = matcher.start();
            final int e = matcher.end();

            int spanStart = s + start;
            int spanEnd = e + start;

            // SuperscriptSpan puts it higher, and RelativeSizeSpan makes the text smaller
            spannable.setSpan(new SuperscriptSpan(), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new RelativeSizeSpan(0.9f), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Hide the original syntax. Using RelativeSizeSpan with 0 as proportion effectively removes the text
            // The syntax is ^(...), so hide 2 characters at the start and one at the end
            spannable.setSpan(new RelativeSizeSpan(0f), spanStart, spanStart + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // This is super hacky and I don't even know why this works, but for nested superscripts it doesn't match completely
            // and it would not hide the last ) unless we go to spanEnd + 1, but if we are at the end we obviously can't
            // go past the end
            if (end == spannable.length()) {
                spannable.setSpan(new RelativeSizeSpan(0f), spanEnd - 1, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                spannable.setSpan(new RelativeSizeSpan(0f), spanEnd - 1, spanEnd + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Apply the superspan recursively as a superspan can have a superspan inside of it
            // Ie. ^(hello ^(there))
            //applySuperSriptSpan(spannable, s + 2, e);
        }
    }

    private void applySuperSriptSpan2(@NonNull Spannable spannable) {
        final String text = spannable.toString();
        final Matcher matcher = RE2.matcher(text);

        while (matcher.find()) {
            int s = matcher.start();
            int e = matcher.end();
            String t = matcher.group();

            // If we are in a sentence superscript ("^()"), the end has an extra parenthesis that should be hidden
            // TODO this wont match actual sentences as the regex matches until a whitespace
            if (t.startsWith("^(") && e != spannable.length()) {
                e++;
            }

            Log.d(TAG, "applySuperSriptSpan2: " + t);

            // SuperscriptSpan puts it higher, and RelativeSizeSpan makes the text smaller
            spannable.setSpan(new SuperscriptSpan(), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new RelativeSizeSpan(0.9f), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            //spannable.setSpan(new RelativeSizeSpan(0f), s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
           // applySuperSriptSpan2(spannable, s + 1, e);
        }
    }
}