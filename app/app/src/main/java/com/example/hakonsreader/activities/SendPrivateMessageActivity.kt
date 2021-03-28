package com.example.hakonsreader.activities

import android.os.Bundle
import com.example.hakonsreader.R
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.databinding.ActivitySendPrivateMessageBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity for sending a private message
 */
@AndroidEntryPoint
class SendPrivateMessageActivity : BaseActivity() {
    companion object {
        private const val TAG = "SendPrivateMessage"

        /**
         * The extras to pre-set the recipient of the message
         *
         * The value for this key should be a [String]
         */
        const val EXTRAS_RECIPIENT = "extras_SendPrivateMessageActivity_recipient"

        /**
         * The extras to pre-set the subject of the message
         *
         * The value for this key should be a [String]
         */
        const val EXTRAS_SUBJECT = "extras_SendPrivateMessageActivity_subject"

        /**
         * The extras to pre-set the message content of the message
         *
         * The value for this key should be a [String]
         */
        const val EXTRAS_MESSAGE = "extras_SendPrivateMessageActivity_message"
    }

    @Inject
    lateinit var api: RedditApi

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

            // By default the recipient has focus
            recipientInput.requestFocusFromTouch()

            // If the recipient is set, pass focus along
            if (recipient.isNotEmpty()) {
                subjectInput.requestFocusFromTouch()

                // Subject is also set, pass focus to the message input
                if (subject.isNotEmpty()) {
                    messageInput.inputView.requestFocusFromTouch()
                }
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