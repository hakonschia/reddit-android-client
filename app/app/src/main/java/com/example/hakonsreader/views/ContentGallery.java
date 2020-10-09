package com.example.hakonsreader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.example.hakonsreader.databinding.ContentGalleryBinding;

public class ContentGallery extends LinearLayout {
    private ContentGalleryBinding binding;

    public ContentGallery(Context context) {
        super(context);
        binding = ContentGalleryBinding.inflate(LayoutInflater.from(context), this, true);
    }
    public ContentGallery(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        binding = ContentGalleryBinding.inflate(LayoutInflater.from(context), this, true);
    }
    public ContentGallery(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        binding = ContentGalleryBinding.inflate(LayoutInflater.from(context), this, true);
    }
    public ContentGallery(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        binding = ContentGalleryBinding.inflate(LayoutInflater.from(context), this, true);
    }
}
