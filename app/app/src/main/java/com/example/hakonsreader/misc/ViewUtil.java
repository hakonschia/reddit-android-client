package com.example.hakonsreader.misc;

import android.content.Context;
import android.content.res.Resources;
import android.view.ViewGroup;
import android.widget.Space;

import androidx.core.content.ContextCompat;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.flairs.RichtextFlair;
import com.example.hakonsreader.views.Tag;

import java.util.List;

public class ViewUtil {
    private ViewUtil() { }


    /**
     * Creates a spoiler tag
     *
     * @param context The context for the tag
     * @return A {@link Tag} with spoiler text and formatting
     */
    public static Tag createSpoilerTag(Context context) {
        Resources resources = context.getResources();

        Tag tag = new Tag(context);
        tag.setFillColor(ContextCompat.getColor(context, R.color.tagSpoiler));
        tag.setTextColor(ContextCompat.getColor(context, R.color.tagSpoilerText));
        tag.addText(resources.getString(R.string.tagSpoiler));

        return tag;
    }

    /**
     * Creates a NSFW tag
     *
     * @param context The context for the tag
     * @return A {@link Tag} with NSFW text and formatting
     */
    public static Tag createNsfwTag(Context context) {
        Resources resources = context.getResources();

        Tag tag = new Tag(context);
        tag.setFillColor(ContextCompat.getColor(context, R.color.tagNSFW));
        tag.setTextColor(ContextCompat.getColor(context, R.color.tagNSFWText));
        tag.addText(resources.getString(R.string.tagNSFW));

        return tag;
    }


    /**
     * Creates a flair for a post or comment
     */
    public static Tag createFlair(List<RichtextFlair> flairs, String flairText, String flairColor, String backgroundColor, Context context) {
        if (flairs == null || flairText == null) {
            return null;
        }

        // No flair to add
        if (flairs.isEmpty() || flairText.isEmpty()) {
            return null;
        }

        Tag tag = new Tag(context);

        int textColor;
        if (flairColor.equals("dark")) {
            textColor = ContextCompat.getColor(context, R.color.flairTextDark);
            tag.setFillColor(ContextCompat.getColor(context, R.color.flairBackgroundDark));
        } else {
            textColor = ContextCompat.getColor(context, R.color.flairTextLight);
            tag.setFillColor(ContextCompat.getColor(context, R.color.flairBackgroundLight));
        }

        if (backgroundColor != null && !backgroundColor.isEmpty()) {
            tag.setFillColor(backgroundColor);
        }

        tag.setTextColor(textColor);

        // If no richtext flairs, try to see if there is a text flair
        // Apparently some subs set both text and richtext flairs *cough* GlobalOffensive *cough*
        // so make sure only one is
        if (flairs.isEmpty()) {
            tag.addText(flairText);
        } else {
            // Add all views the flair has
            flairs.forEach(flair -> {
                if (flair.getType().equals("text")) {
                    tag.addText(flair.getText());
                } else if (flair.getType().equals("emoji")) {
                    tag.addImage(flair.getUrl());
                }
            });
        }

        return tag;
    }


    /**
     * Adds a tag to the layout, with extra space after it
     *
     * @param layout The layout to add the tag to
     * @param tag The tag to add
     */
    public static void addTagWithSpace(ViewGroup layout, Tag tag) {
        layout.addView(tag);

        Space space = new Space(layout.getContext());
        space.setMinimumWidth((int)layout.getResources().getDimension(R.dimen.tagSpace));
        layout.addView(space);
    }

}
