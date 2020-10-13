package com.example.hakonsreader.markwonplugins;

import android.graphics.Color;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.utils.ColorUtils;


/**
 * Markwon plugin for Reddit spoilers
 *
 * <p>Taken from the Markown samples: https://github.com/noties/Markwon/blob/master/app-sample/src/main/java/io/noties/markwon/app/samples/RedditSpoilerSample.java</p>
 */
public class RedditSpoilerPlugin extends AbstractMarkwonPlugin {

    private static final Pattern RE = Pattern.compile(">!.+?!<");

    @NonNull
    @Override
    public String processMarkdown(@NonNull String markdown) {
        // replace all `>!` with `&gt;!` so no blockquote would be parsed (when spoiler starts at new line)
        return markdown.replaceAll(">!", "&gt;!");
    }

    @Override
    public void beforeSetText(@NonNull TextView textView, @NonNull Spanned markdown) {
        applySpoilerSpans((Spannable) markdown);
    }

    private static void applySpoilerSpans(@NonNull Spannable spannable) {
        final String text = spannable.toString();
        final Matcher matcher = RE.matcher(text);

        while (matcher.find()) {

            final RedditSpoilerSpan spoilerSpan = new RedditSpoilerSpan();
            final ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    spoilerSpan.setRevealed(true);
                    widget.postInvalidateOnAnimation();
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    // no op
                }
            };

            final int s = matcher.start();
            final int e = matcher.end();
            spannable.setSpan(spoilerSpan, s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(clickableSpan, s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // we also can hide original syntax
            spannable.setSpan(new RelativeSizeSpan(0f), s, s + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new RelativeSizeSpan(0f), e - 2, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static class RedditSpoilerSpan extends CharacterStyle {

        private boolean revealed;

        @Override
        public void updateDrawState(TextPaint tp) {
            if (!revealed) {
                // use the same text color
                tp.bgColor = Color.GRAY;
                tp.setColor(Color.GRAY);
            } else {
                // for example keep a bit of black background to remind that it is a spoiler
                tp.bgColor = ColorUtils.applyAlpha(Color.BLACK, 25);
            }
        }

        public void setRevealed(boolean revealed) {
            this.revealed = revealed;
        }
    }
}