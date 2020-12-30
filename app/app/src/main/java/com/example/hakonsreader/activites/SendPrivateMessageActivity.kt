package com.example.hakonsreader.activites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.databinding.ActivitySendPrivateMessageBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Activity for sending a private message
 */
class SendPrivateMessageActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SendPrivateMessage"

        /**
         * The extras to pre-set the recipient of the message
         *
         * The value for this key should be a [String]
         */
        const val EXTRAS_RECIPIENT = "recipient"

        /**
         * The extras to pre-set the subject of the message
         *
         * The value for this key should be a [String]
         */
        const val EXTRAS_SUBJECT = "subject"

        /**
         * The extras to pre-set the message content of the message
         *
         * The value for this key should be a [String]
         */
        const val EXTRAS_MESSAGE = "message"
    }

    private lateinit var binding: ActivitySendPrivateMessageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendPrivateMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val extras = intent.extras

        val recipient = extras?.getString(EXTRAS_RECIPIENT) ?: ""
        val subject = extras?.getString(EXTRAS_SUBJECT) ?: ""
        val message = extras?.getString(EXTRAS_MESSAGE) ?: ""

        binding.recipientInput.setText(recipient)
        binding.subjectInput.setText(subject)
        binding.messageInput.setText(message)

        binding.showPreview.setOnClickListener {
            binding.messageInput.showPreviewInPopupDialog()
        }

        binding.sendMessage.setOnClickListener {
            if (!verifyInputFieldsNotEmpty()) {
                return@setOnClickListener
            }

            sendMessage()
        }
    }

    /**
     * Verifies that all the input fields are not empty
     *
     * @return False if one of the input fields are empty
     */
    private fun verifyInputFieldsNotEmpty() : Boolean {
        var returnValue = true

        if (binding.recipientInput.text?.isBlank() == true) {
            binding.recipientInput.error = getString(R.string.sendPrivateMessageMissingRecipient)
            returnValue = false
        }

        if (binding.subjectInput.text?.isBlank() == true) {
            binding.subjectInput.error = getString(R.string.sendPrivateMessageMissingSubject)
            returnValue = false
        }

        if (binding.messageInput.inputText.isBlank()) {
            //  binding.messageInput.error = getString(R.string.sendPrivateMessageMissingRecipient)
            returnValue = false
        }

        return returnValue
    }

    private fun sendMessage() {
        val api = App.get().api

        CoroutineScope(Dispatchers.IO).launch {
            val recipient = binding.recipientInput.text.toString()
            val subject = binding.subjectInput.text.toString()
            val message = binding.messageInput.inputText

            api.messages().sendMessage(recipient, subject, message)
        }
    }
}