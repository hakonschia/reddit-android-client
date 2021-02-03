package com.example.hakonsreader.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.hakonsreader.R
import com.example.hakonsreader.misc.startLoginIntent

/**
 * Fragment for logging in
 */
class LogInFragment : Fragment() {
    companion object {
        /**
         * @return A new instance of this fragment
         */
        fun newInstance() = LogInFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_log_in, container, false).apply {
            findViewById<Button>(R.id.btnLogIn).setOnClickListener { startLoginIntent(context) }
        }
    }
}