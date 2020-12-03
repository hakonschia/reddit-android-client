package com.example.hakonsreader.activites

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.hakonsreader.R
import com.example.hakonsreader.fragments.LogInFragment

class LogInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LogInFragment())
                .commit()
    }
}