package com.example.hakonsreader.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity for testing custom views. Use [layout] to specify the layout file to inflate
 */
@AndroidEntryPoint
class MockActivity : AppCompatActivity() {
    companion object {
        var layout: Int = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout)
    }
}