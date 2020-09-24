package com.example.hakonsreader.misc;

import android.content.Context;
import android.content.res.Resources;

import com.example.hakonsreader.R;
import com.example.hakonsreader.views.Tag;

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
        tag.setText(resources.getString(R.string.tagSpoiler));
        tag.setTextColor(resources.getColor(R.color.tagSpoilerText));
        tag.setFillColor(resources.getColor(R.color.tagSpoiler));

        return tag;
    }

    /**
     * Creates a NSFW tag
     *
     * @param context The context for the tag
     * @return A {@link Tag} with NSFW text and formatting
     */
    public static Tag createNSFWTag(Context context) {
        Resources resources = context.getResources();

        Tag tag = new Tag(context);
        tag.setText(resources.getString(R.string.tagNSFW));
        tag.setTextColor(resources.getColor(R.color.tagNSFWText));
        tag.setFillColor(resources.getColor(R.color.tagNSFW));

        return tag;
    }

}
