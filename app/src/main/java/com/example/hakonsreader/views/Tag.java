package com.example.hakonsreader.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.example.hakonsreader.R;
import com.example.hakonsreader.databinding.TagBinding;

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
            textColor = a.getColor(R.styleable.Tag_textColor, getResources().getColor(R.color.textColor));
            fillColor = a.getColor(R.styleable.Tag_textColor, getResources().getColor(R.color.background));
        } finally {
            a.recycle();
        }
    }

    public Tag(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public Tag(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    public void setText(String text) {
        binding.text.setText(text);
    }

    public void setTextColor(int color) {
        binding.text.setTextColor(color);
    }
    public void setTextColor(String hexColor) {
        binding.text.setTextColor(Color.parseColor(hexColor));
    }


    public void setFillColor(int color) {
        binding.cardView.setCardBackgroundColor(color);
    }
    public void setFillColor(String hexColor) {
        binding.cardView.setCardBackgroundColor(Color.parseColor(hexColor));
    }
}
