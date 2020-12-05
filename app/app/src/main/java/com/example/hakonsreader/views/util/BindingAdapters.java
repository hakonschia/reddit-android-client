package com.example.hakonsreader.views.util;

import android.widget.TextView;

import androidx.databinding.BindingAdapter;

import com.example.hakonsreader.App;
import com.example.hakonsreader.misc.InternalLinkMovementMethod;
import com.example.hakonsreader.misc.Util;

import java.time.Duration;
import java.time.Instant;

import javax.annotation.Nullable;


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
        markdown = App.get().getAdjuster().adjust(markdown);
        App.get().getMark().setMarkdown(textView, markdown);
    }
}
