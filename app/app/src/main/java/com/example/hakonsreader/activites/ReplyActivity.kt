package com.example.hakonsreader.activites

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.activites.ReplyActivity.Companion.LISTING_KEY
import com.example.hakonsreader.activites.ReplyActivity.Companion.LISTING_KIND_KEY
import com.example.hakonsreader.api.enums.PostType
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.interfaces.ReplyableListing
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.databinding.ActivityReplyBinding
import com.example.hakonsreader.misc.Util
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity to reply to a post or comment
 *
 * The post/comment should be passed to the activity with the key [LISTING_KEY] and the
 * what kind of listing it is with [LISTING_KIND_KEY]
 */
class ReplyActivity : AppCompatActivity() {
    private val TAG = "ReplyActivity"

    companion object {
        public const val LISTING_KEY = "replyingToListing"

        public const val LISTING_KIND_KEY = "replyingToListingKind"

        /**
         * The key used used to store if the confirm discard dialog is shown
         */
        private const val CONFIRM_DIALOG_SHOWN = "confirmDialogShown"

        /**
         * Key used to store the state of the reply text
         */
        private const val REPLY_TEXT = "replyText"

        /**
         * Key used to store if the URL dialog is shown
         */
        private const val LINK_DIALOG_SHOWN = "urlDialogShown"

        /**
         * Key used to store the state of the URL dialog link text
         */
        private const val LINK_DIALOG_TEXT = "urlDialogText"

        /**
         * Key used to store the state of the URL dialog link
         */
        private const val LINK_DIALOG_LINK = "urlDialogLink"
    }


    var binding: ActivityReplyBinding? = null
    private val api = App.get().api

    /**
     * The listing (comment or post) being replied to
     */
    private var replyingTo: ReplyableListing? = null

    /**
     * Dialog displayed when the user wants to finish the activity with text in the input field
     * that ensures the user wants to discard the text
     */
    private var confirmDiscardDialog: Dialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReplyBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        binding?.showPreview?.setOnClickListener { showPreview() }

        if (savedInstanceState != null) {
            restoreInstanceState(savedInstanceState)
        }

        val extras = intent.extras
        if (extras == null) {
            finish()
            return
        }

        setListingFromExtras(extras)
        showNotLoggedInDialogIfNotLoggedIn()
    }

    override fun onResume() {
        super.onResume()
        App.get().setActiveActivity(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(REPLY_TEXT, binding?.markdownInput?.inputText)

        if (binding?.markdownInput?.isLinkDialogShown == true) {
            outState.putBoolean(LINK_DIALOG_SHOWN, true)

            outState.putString(LINK_DIALOG_TEXT, binding?.markdownInput?.linkDialogText)
            outState.putString(LINK_DIALOG_LINK, binding?.markdownInput?.linkDialogLink)

            // Ensure the dialog is dismissed or else it will cause a leak
            binding?.markdownInput?.dismissLinkDialog()
        }

        if (confirmDiscardDialog?.isShowing == true) {
            outState.putBoolean(CONFIRM_DIALOG_SHOWN, true)
            confirmDiscardDialog?.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    /**
     * If there is text a dialog is shown to warn the user that they are leaving text behind
     * and makes the user confirm they want to discard the text
     */
    override fun finish() {
        if (binding?.markdownInput?.inputText.isNullOrBlank()) {
            super.finish()
        } else {
            showConfirmDialog()
        }

        // Might not actually finish, but it shouldn't matter
        overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
    }


    /**
     * Restores a saved instance state
     *
     * @param state The state to restore
     */
    private fun restoreInstanceState(state: Bundle) {
        binding?.markdownInput?.setText(state.getString(REPLY_TEXT, ""))

        // Restore the link dialog
        val showLinkDialog = state.getBoolean(LINK_DIALOG_SHOWN)
        if (showLinkDialog) {
            val text = state.getString(LINK_DIALOG_TEXT, "")
            val link = state.getString(LINK_DIALOG_LINK, "")
            binding?.markdownInput?.showLinkDialog(text, link)
        }

        // Restore the confirmation for discarding dialog
        val showConfirmDialog = state.getBoolean(CONFIRM_DIALOG_SHOWN)
        if (showConfirmDialog) {
            showConfirmDialog()
        }
    }

    /**
     * Sets [replyingTo] from a bundle
     *
     * @param extras The bundle to set the listing from
     */
    private fun setListingFromExtras(extras: Bundle) {
        val jsonData = extras.getString(LISTING_KEY)
        val kind = extras.getString(LISTING_KIND_KEY)

        if (kind == Thing.POST.value) {
            replyingTo = Gson().fromJson(jsonData, RedditPost::class.java)
            replyingTo.let {
                it as RedditPost

                // If the post is a selftext post then use that as the summary, otherwise
                // use the title
                if (it.getPostType() == PostType.TEXT) {
                    App.get().mark.setMarkdown(binding!!.summary, it.selftext)
                } else {
                    binding?.summary?.text = it.title
                }
            }
        } else {
            replyingTo = Gson().fromJson(jsonData, RedditComment::class.java)
            replyingTo.let {
                it as RedditComment
                App.get().mark.setMarkdown(binding!!.summary, it.body)
            }
        }

        binding?.listing = replyingTo
    }

    /**
     * Shows a dialog to let the user confirm they want to leave
     *
     *
     * If the user selects to leave, [finish] is called
     */
    private fun showConfirmDialog() {
        if (confirmDiscardDialog == null) {
            confirmDiscardDialog = Dialog(this)
            confirmDiscardDialog!!.setContentView(R.layout.dialog_reply_confirm_back_press)
        }
        // TODO causes a leak when discard is clicked
        confirmDiscardDialog!!.show()

        // Because using match_parent in the layout file doesn't actually match the parent (screen width)
        // This looks weird on horizontal orientation though
        confirmDiscardDialog!!.window!!.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val discard = confirmDiscardDialog!!.findViewById<Button>(R.id.btnDiscard)
        val cancel = confirmDiscardDialog!!.findViewById<Button>(R.id.btnCancel)
        discard.setOnClickListener { super.finish() }
        cancel.setOnClickListener { confirmDiscardDialog!!.dismiss() }

        // TODO add button for "discard and save" that saves the text and whats being responded to so we can resume later
    }

    /**
     * If there is no user logged in a dialog is shown to the user that
     * they are not logged in and won't be able to send a reply
     */
    private fun showNotLoggedInDialogIfNotLoggedIn() {
        // TODO private browsing
        if (!App.get().isUserLoggedIn) {
            AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialogReplyNotLoggedInTitle))
                    .setMessage(getString(R.string.dialogReplyNotLoggedInContent))
                    .show()
        }
    }

    /**
     * Shows a preview of the reply text in a dialog
     */
    private fun showPreview() {
        // TODO this should use a custom dialog to be able to render the custom markdown plugins
        //  also copy it to SubmitActivity when it's done (should maybe add a function in markdownInput to show the preview instead)
        val asMarkdown = binding?.markdownInput?.inputText?.let { App.get().mark.toMarkdown(it) }

        AlertDialog.Builder(this)
                .setTitle(R.string.preview)
                .setMessage(asMarkdown)
                .show()
    }


    /**
     * Sends the reply
     *
     * @param view Ignored
     */
    public fun sendReply(view: View) {
        val text = binding?.markdownInput?.inputText

        // TODO add text change listener and disable button if empty
        if (text.isNullOrBlank() || replyingTo == null) {
            return
        }

        // Hide the keyboard
        val v = currentFocus
        if (v != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)
        }

        replyingTo?.let {
            val id = it.id

            CoroutineScope(IO).launch {
                val response = if (replyingTo is RedditPost) {
                    api.postKt(id)
                } else {
                    api.commentKt(id)
                }.reply(text)

                when (response) {
                    is ApiResponse.Success -> withContext(Main) { replySuccess(response.value) }
                    is ApiResponse.Error -> Util.handleGenericResponseErrors(binding?.parentLayout, response.error, response.throwable)
                }
            }
        }
    }

    /**
     * Handles success to posting a reply. This will pass the new comment back by calling [setResult]
     * and [finish]
     *
     * This must run on the main thread
     *
     * @param comment The new comment received from the API
     */
    private fun replySuccess(comment: RedditComment) {
        // No depth set means we're replying to a comment, set depth manually based on the parent
        if (comment.depth == -1) {
            comment.depth = (replyingTo as RedditComment).depth + 1
        }

        // Pass the new comment back and finish
        val intent = intent.putExtra(LISTING_KEY, Gson().toJson(comment))

        // Kind of a bad way to do it, but if we call finish with text in the input a dialog is shown
        // Other option is to create a flag (ie "replySent") and not show the dialog if true
        binding?.markdownInput?.clearText()
        setResult(RESULT_OK, intent)
        finish()
    }
}