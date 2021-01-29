package com.example.hakonsreader.views.util;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.databinding.BindingAdapter;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.misc.InternalLinkMovementMethod;
import com.example.hakonsreader.misc.Util;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;


/**
 * Contains the static binding adapters used for data binding that aren't specific to one view
 */
public class BindingAdapters {

    private BindingAdapters() { }

    /**
     * Binding adapter for setting the age text on the post. The text is formatted as "2 hours", "1 day" etc.
     * For a shortened text (5m etc.) use {@link BindingAdapters#setAgeTextShortened(TextView, long)}
     *
     * @param textView The textView to set the text on
     * @param createdAt The timestamp the post was created at. If this is negative nothing is done
     */
    @BindingAdapter({"createdAt"})
    public static void setAgeText(TextView textView, long createdAt) {
        if (createdAt >= 0) {
            Instant created = Instant.ofEpochSecond(createdAt);
            Instant now = Instant.now();
            Duration between = Duration.between(created, now);

            textView.setText(Util.createAgeText(textView.getResources(), between));
        }
    }

    /**
     * Binding adapter for setting a text for when something was created. The text is formatted as
     * "Created 1. january 1970"
     *
     * @param textView The textView to set the text on
     * @param createdAt The timestamp (seconds) the post was created at. If this is negative nothing is done
     */
    @BindingAdapter({"createdAtFullText"})
    public static void setCreatedAtFullText(TextView textView, long createdAt) {
        if (createdAt >= 0) {
            SimpleDateFormat format = new SimpleDateFormat("d. MMMM y", Locale.getDefault());
            // Seconds is given, milliseconds is expected
            Date date = new Date(createdAt * 1000L);

            String full = textView.getResources().getString(R.string.subredditInfoCreatedAt, format.format(date));
            textView.setText(full);
        }
    }

    /**
     * Binding adapter for setting the age text on the post. The text is formatted as "*2 hours", "*1 day" etc.
     * For a shortened text (*5m etc.) use {@link BindingAdapters#setAgeTextEditedShortened(TextView, long)}
     *
     * @param textView The textView to set the text on
     * @param createdAt The timestamp the post was created at. If this is negative nothing is done
     */
    @BindingAdapter({"editedAt"})
    public static void setAgeTextEdited(TextView textView, long createdAt) {
        if (createdAt >= 0) {
            Instant created = Instant.ofEpochSecond(createdAt);
            Instant now = Instant.now();
            Duration between = Duration.between(created, now);

            textView.setText("*" + Util.createAgeText(textView.getResources(), between));
        }
    }


    /**
     * Binding adapter for setting the age text on the post. The text is formatted as "2h", "1d" etc..
     * For a longer text (5 minutes etc.) use {@link BindingAdapters#setAgeText(TextView, long)}
     *
     * @param textView The textView to set the text on
     * @param createdAt The timestamp the post was created at. If this is negative nothing is done
     */
    @BindingAdapter({"createdAtShortened"})
    public static void setAgeTextShortened(TextView textView, long createdAt) {
        if (createdAt >= 0) {
            Instant created = Instant.ofEpochSecond(createdAt);
            Instant now = Instant.now();
            Duration between = Duration.between(created, now);

            textView.setText(Util.createAgeTextShortened(textView.getResources(), between));
        }
    }

    /**
     * Binding adapter for setting the time when a post/comment was edited. The text is formatted as "*2h", "*1d" etc..
     * For a longer text (5 minutes etc.) use {@link BindingAdapters#setAgeTextEdited(TextView, long)} (TextView, long)}
     *
     * @param textView The textView to set the text on
     * @param createdAt The timestamp the post was created at. If this is negative nothing is done
     */
    @BindingAdapter({"editedAtShortened"})
    public static void setAgeTextEditedShortened(TextView textView, long createdAt) {
        if (createdAt >= 0) {
            Instant created = Instant.ofEpochSecond(createdAt);
            Instant now = Instant.now();
            Duration between = Duration.between(created, now);

            textView.setText("*" + Util.createAgeTextShortened(textView.getResources(), between));
        }
    }

    /**
     * Sets a TextView with markdown text.
     *
     * <p>The markdown will be adjusted with {@link App#getAdjuster()}</p>
     *
     * <p>The TextView will have its movement method set to {@link InternalLinkMovementMethod}</p>
     *
     * @param textView The TextView to add the markdown to
     * @param markdown The markdown text
     */
    @BindingAdapter("markdown")
    public static void setMarkdown(TextView textView, @Nullable String markdown) {
        if (markdown == null) {
            return;
        }

        textView.setMovementMethod(InternalLinkMovementMethod.getInstance(textView.getContext()));
        markdown = App.Companion.get().getAdjuster().adjust(markdown);
        App.Companion.get().getMarkwon().setMarkdown(textView, markdown);
    }

    /**
     * Sets the text color of a TextView based on a reddit distinguish. By default this will use "text_color"
     *
     * @param textView The text view to set the color on
     * @param distinguish A string defining how the thing is distinguished. If this is null the default color is used
     */
    @BindingAdapter("textColorFromRedditDistinguish")
    public static void setTextColorFromRedditDistinguish(TextView textView, @Nullable String distinguish) {
        int color = R.color.text_color;

        Context context = textView.getContext();

        if (distinguish == null) {
            textView.setTextColor(ContextCompat.getColor(context, color));
            return;
        }

        if (distinguish.equals("admin")) {
            color = R.color.commentByAdminBackground;
        } else if (distinguish.equals("moderator")) {
            color = R.color.commentByAdminBackground;
        }

        textView.setTextColor(ContextCompat.getColor(context, color));
    }
}
