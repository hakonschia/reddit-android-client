package com.example.hakonsreader.fragments.bottomsheets

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.hakonsreader.databinding.BottomSheetSendPrivateMessageBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * BottomSheet for sending a private message
 */
class SendPrivateMessageBottomSheet : BottomSheetDialogFragment() {
    companion object {
        private const val TAG = "SendPrivateMessageBottomSheet"
    }

    private var _binding: BottomSheetSendPrivateMessageBinding? = null
    private val binding get() = _binding!!

    var recipient = ""
    var subject = ""
    var message = ""

    /**
     * The Runnable to run when the bottom sheet has been dismissed
     */
    var onDismiss: Runnable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = BottomSheetSendPrivateMessageBinding.inflate(layoutInflater)

        binding.recipientInput.setText(recipient)
        binding.subjectInput.setText(subject)
        binding.messageInput.setText(message)

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
}