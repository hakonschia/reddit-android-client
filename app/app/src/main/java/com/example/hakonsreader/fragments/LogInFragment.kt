package com.example.hakonsreader.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.constants.NetworkConstants

/**
 * Fragment for logging in
 */
class LogInFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_log_in, container, false)

        view.findViewById<Button>(R.id.btnLogIn).setOnClickListener { openOAuthAuthIntent() }

        return view
    }

    /**
     * Opens a web page prompting the user to authenticate the application
     */
    private fun openOAuthAuthIntent() {
        // Generate a new state to validate when we get a response back
        val state = App.get().generateAndGetOAuthState()

        val uri = Uri.Builder()
                .scheme("https")
                .authority("reddit.com")
                .path("api/v1/authorize")
                .appendQueryParameter("response_type", NetworkConstants.RESPONSE_TYPE)
                .appendQueryParameter("duration", NetworkConstants.DURATION)
                .appendQueryParameter("redirect_uri" , NetworkConstants.CALLBACK_URL)
                .appendQueryParameter("client_id", NetworkConstants.CLIENT_ID)
                .appendQueryParameter("scope", NetworkConstants.SCOPE)
                .appendQueryParameter("state", state)
                .build()

        val assstring = uri.toString()

        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}