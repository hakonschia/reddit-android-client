package com.example.hakonsreader.activities

import android.app.AlertDialog
import android.os.Bundle
import com.example.hakonsreader.R

/**
 * This activity only performs one job: displaying an alert dialog to notify a logged in user that
 * the access token isn't valid anymore, and that the user has been logged out.
 *
 * This activity has a transparent background so the alert dialog will be the only visible item and
 * appear above the current active activity
 */
class InvalidAccessTokenActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AlertDialog.Builder(this)
                .setTitle(R.string.applicationAccessRevokedHeader)
                .setMessage(R.string.applicationAccessRevokedContent)
                .setOnDismissListener {
                    finish()
                }
                .show()
    }

}