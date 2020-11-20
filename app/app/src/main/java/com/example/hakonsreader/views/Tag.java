package com.example.hakonsreader.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
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

public class Tag extends LinearLayout {
    private static final String TAG = "Tag";
    
    private TagBinding binding;
    private String text;
    private int textColor;
    private int fillColor;

    public Tag(Context context) {
        this(context, null, 0);
    }
    public Tag(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public Tag(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.Tag, 0, 0);
        try {
            text = a.getString(R.styleable.Tag_text);
            textColor = a.getColor(R.styleable.Tag_textColor, ContextCompat.getColor(context, R.color.text_color));
            fillColor = a.getColor(R.styleable.Tag_textColor, ContextCompat.getColor(context, R.color.background));
        } finally {
            a.recycle();
        }

        binding = TagBinding.inflate(LayoutInflater.from(context), this, true);
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
        if (hexColor.equals("transparent")) {
            hexColor = "#80000000";
        }

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
     * @param text The text to add. If this is empty nothing is added to the tag
     */
    public void addText(String text) {
        if (text.isEmpty()) {
            return;
        }

        // If the last item in the tags is a TextView, add the text to that view instead
        // of creating a new view
        int tagsAdded = binding.tags.getChildCount();
        if (tagsAdded > 0) {
            View last = binding.tags.getChildAt(tagsAdded - 1);

            if (last instanceof TextView) {
                TextView asTextView = (TextView) last;
                asTextView.append(" " + text);

                return;
            }
        }

        TextView tv = new TextView(getContext());

        tv.setText(text);
        tv.setTextColor(textColor);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, getContext().getResources().getDimension(R.dimen.tagTextSize));

        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setMaxLines(1);

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
