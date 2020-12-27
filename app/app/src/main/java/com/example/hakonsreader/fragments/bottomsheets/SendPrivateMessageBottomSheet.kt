package com.example.hakonsreader.fragments.bottomsheets

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.hakonsreader.R
import com.example.hakonsreader.activites.SubmitActivity
import com.example.hakonsreader.databinding.BottomSheetSendPrivateMessageBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar

/**
 * BottomSheet for sending a private message
 */
class SendPrivateMessageBottomSheet : BottomSheetDialogFragment() {
    companion object {
        private const val TAG = "SendPrivateMessageBottomSheet"
    }

    private var _binding: BottomSheetSendPrivateMessageBinding? = null
    private val binding get() = _binding!!

    /**
     * The recipient of the message. Set this before the BottomSheet is shown to pre-set a recipient
     */
    var recipient: String? = null

    /**
     * The subject of the message. Set this before the BottomSheet is shown to pre-set the subject
     */
    var subject: String? = null

    /**
     * The content of the message. Set this before the BottomSheet is shown to pre-set the content
     */
    var message: String? = null

    /**
     * The Runnable to run when the bottom sheet has been dismissed
     */
    var onDismiss: Runnable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = BottomSheetSendPrivateMessageBinding.inflate(layoutInflater)

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
        }

        return binding.root
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismiss?.run()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
}