package com.example.hakonsreader.viewmodels.factories;

import android.content.Context;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.hakonsreader.viewmodels.SelectSubredditsViewModel;

public class SelectSubredditsFactory implements ViewModelProvider.Factory {
    private Context context;

    public SelectSubredditsFactory(Context context) {
        this.context = context;
    }

    @Override
    public <T extends ViewModel> T create(Class<T> modelClass) {
        return (T) new SelectSubredditsViewModel(context);
    }
}