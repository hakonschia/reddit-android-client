package com.example.hakonsreader.misc;

import android.content.Context;
import android.content.res.Resources;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

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

        TextView tv = new TextView(context);
        tv.setText(resources.getString(R.string.tagSpoiler));
        tv.setTextSize(resources.getDimension(R.dimen.tagTextSize));
        tv.setTextColor(ContextCompat.getColor(context, R.color.tagSpoilerText));

        Tag tag = new Tag(context);
        tag.add(tv);
        tag.setFillColor(ContextCompat.getColor(context, R.color.tagSpoiler));

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

        TextView tv = new TextView(context);
        tv.setText(resources.getString(R.string.tagNSFW));
        tv.setTextSize(resources.getDimension(R.dimen.tagTextSize));
        tv.setTextColor(ContextCompat.getColor(context, R.color.tagNSFWText));

        Tag tag = new Tag(context);
        tag.add(tv);
        tag.setFillColor(ContextCompat.getColor(context, R.color.tagNSFW));

        return tag;
    }

}
