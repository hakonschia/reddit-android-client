/**
 * All extensions taken from:
 * https://github.com/tfcporciuncula/hilt-assisted-injection/blob/master/app/src/main/java/com/tfcporciuncula/hiltassistedinjection/AssistedViewModel.kt
 */

@file:Suppress("UNCHECKED_CAST")

package com.example.hakonsreader.viewmodels

import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel


inline fun <reified T : ViewModel> FragmentActivity.assistedViewModel(
        crossinline viewModelProducer: (SavedStateHandle) -> T
) = viewModels<T> {
    object : AbstractSavedStateViewModelFactory(this@assistedViewModel, intent?.extras) {
        override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle) =
                viewModelProducer(handle) as T
    }
}

inline fun <reified T : ViewModel> Fragment.assistedViewModel(
        crossinline viewModelProducer: (SavedStateHandle) -> T
) = viewModels<T> {
    object : AbstractSavedStateViewModelFactory(this@assistedViewModel, arguments) {
        override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle) =
                viewModelProducer(handle) as T
    }
}

inline fun <reified T : ViewModel> Fragment.assistedActivityViewModel(
        crossinline viewModelProducer: (SavedStateHandle) -> T
) = activityViewModels<T> {
    object : AbstractSavedStateViewModelFactory(this@assistedActivityViewModel, activity?.intent?.extras) {
        override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle) =
                viewModelProducer(handle) as T
    }
}