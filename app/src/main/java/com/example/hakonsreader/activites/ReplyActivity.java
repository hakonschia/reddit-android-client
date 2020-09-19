package com.example.hakonsreader.activites;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.api.interfaces.RedditListing;
import com.example.hakonsreader.databinding.ActivityReplyBinding;

public class ReplyActivity extends AppCompatActivity {
    private ActivityReplyBinding binding;

    private RedditListing replyingTo;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = ActivityReplyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


    }
}
