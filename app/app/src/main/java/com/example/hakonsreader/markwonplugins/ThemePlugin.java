package com.example.hakonsreader.markwonplugins;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.hakonsreader.R;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.core.MarkwonTheme;


/**
 * Markwon plugin that defines the theme
 *
 * <p>Sets block quote theme</p>
 * <p>Sets code theme</p>
 */
public class ThemePlugin extends AbstractMarkwonPlugin {
    private Context context;

    public ThemePlugin(Context context) {
        this.context = context;
    }

    @Override
    public void configureTheme(@NonNull MarkwonTheme.Builder builder) {
        builder
                .blockQuoteColor(ContextCompat.getColor(context, R.color.quoteLine))
                .blockMargin((int)context.getResources().getDimension(R.dimen.quoteMargin))
                .blockQuoteWidth((int)context.getResources().getDimension(R.dimen.quoteWidth))
                .thematicBreakHeight((int)context.getResources().getDimension(R.dimen.thematicBreakHeight))
                .linkColor(ContextCompat.getColor(context, R.color.linkColor))
                .build();
    }
}
