package com.example.hakonsreader.activities

import android.os.Bundle
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.databinding.ActivitySendPrivateMessageBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Activity for sending a private message
 */
class SendPrivateMessageActivity : BaseActivity() {
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

        with(binding) {
            recipientInput.setText(recipient)
            subjectInput.setText(subject)
            messageInput.setText(message)

            // Set focus to the input field after the last pre-filled field
            when {
                recipient.isNotEmpty() -> subjectInput.requestFocusFromTouch()
                subject.isNotEmpty() -> messageInput.requestFocusFromTouch()
                else -> recipientInput.requestFocusFromTouch()
            }

            showPreview.setOnClickListener {
                messageInput.showPreviewInPopupDialog()
            }

            sendMessage.setOnClickListener {
                if (!verifyInputFieldsNotEmpty()) {
                    return@setOnClickListener
                }

                sendMessage()
            }
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
            with(binding) {
                val recipient = recipientInput.text.toString()
                val subject = subjectInput.text.toString()
                val message = messageInput.inputText

                api.messages().sendMessage(recipient, subject, message)
            }
        }
    }
}