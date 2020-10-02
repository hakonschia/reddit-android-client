package com.example.hakonsreader.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.hakonsreader.R;
import com.example.hakonsreader.databinding.TagBinding;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class Tag extends LinearLayout {
    private TagBinding binding;
    private String text;
    private int textColor;
    private int fillColor;

    public Tag(Context context) {
        super(context);

        LayoutInflater inflater = LayoutInflater.from(context);
        binding = TagBinding.inflate(inflater, this, true);
    }

    public Tag(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.Tag, 0, 0);

        try {
            text = a.getString(R.styleable.Tag_text);
            textColor = a.getColor(R.styleable.Tag_textColor, ContextCompat.getColor(context, R.color.textColor));
            fillColor = a.getColor(R.styleable.Tag_textColor, ContextCompat.getColor(context, R.color.background));
        } finally {
            a.recycle();
        }
    }
    public Tag(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Adds a view to the flair
     *
     * <p>If the tag has multiple views ensure they are added in the correct order</p>
     *
     * <p>Padding is added to the view</p>
     */
    public void add(View view) {
        view.setPadding(5, 0, 5, 1);
        binding.tags.addView(view);
    }

    /**
     * Sets the fill/background color of the tag
     *
     * @param color The color resource ID
     */
    public void setFillColor(int color) {
        binding.cardView.setCardBackgroundColor(color);
    }

    /**
     * Sets the fill/background color of the tag
     *
     * @param hexColor A hex string representing a color
     */
    public void setFillColor(String hexColor) {
        binding.cardView.setCardBackgroundColor(Color.parseColor(hexColor));
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    /**
     * Adds text to the tag
     *
     * <p>Note the text color must be set with {@link Tag#setTextColor(int)} before this is called</p>
     *
     * @param text The text to add
     */
    public void addText(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setTextColor(textColor);
        tv.setTextSize(getContext().getResources().getDimension(R.dimen.tagTextSize));

        this.add(tv);
    }

    /**
     * Adds an image to the tag
     *
     * @param imageURL The URL to the image
     */
    public void addImage(String imageURL) {
        int size = (int)getContext().getResources().getDimension(R.dimen.tagIconSize);
        ImageView iv = new ImageView(getContext());
        Picasso.get()
                .load(imageURL)
                .resize(size, size)
                .into(iv);

        this.add(iv);
    }
}
