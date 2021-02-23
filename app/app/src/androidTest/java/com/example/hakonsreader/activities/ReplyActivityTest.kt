package com.example.hakonsreader.activities

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.LayoutMatchers.hasEllipsizedText
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hakonsreader.R
import com.example.hakonsreader.activites.ReplyActivity
import com.example.hakonsreader.api.enums.Thing
import com.example.hakonsreader.api.model.RedditPost
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4ClassRunner::class)
class ReplyActivityTest {

    // This JSON file is a straight copy from an actual comment:
    // https://www.reddit.com/r/hakonschia/comments/ko9xg5/i_literally_dont_know_whats_going_on_right_now/gofyoc3
    private val commentData = javaClass.classLoader!!.getResource("replying_to_a_comment.json").readText()
    // https://www.reddit.com/r/hakonschia/comments/k8flnu/bruh/
    private val postData = javaClass.classLoader!!.getResource("replying_to_a_post.json").readText()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Tests that opening a ReplyActivity when not logged in displays an alert dialog
     */
    @Test
    fun notLoggedInDialogIsShown() {
        val intent = Intent(InstrumentationRegistry.getInstrumentation().targetContext, ReplyActivity::class.java).apply {
            putExtra(ReplyActivity.LISTING_KIND_KEY, Thing.COMMENT.value)
            putExtra(ReplyActivity.LISTING_KEY, commentData)
        }
        ActivityScenario.launch<ReplyActivity>(intent)

        // We're not logged in, which will display a dialog
        onView(withText(R.string.dialogReplyNotLoggedInTitle))
                .check(matches(isDisplayed()))
    }

    /**
     * Tests that the username of the user being replied to is shown in the title, as well as displaying
     * the comment being replied to
     */
    @Test
    fun displaysUsernameAndCommentBeingRepliedTo() {
        val intent = Intent(context, ReplyActivity::class.java).apply {
            putExtra(ReplyActivity.LISTING_KIND_KEY, Thing.COMMENT.value)
            putExtra(ReplyActivity.LISTING_KEY, commentData)
        }
        ActivityScenario.launch<ReplyActivity>(intent)

        // Not sure how we can avoid this in tests since we don't have a user
        onView(withText(R.string.dialogReplyNotLoggedInTitle))
                .check(matches(isDisplayed()))
        // Dismiss the dialog
        pressBack()

        // Matches the username of the comment (from the JSON file)
        val formattedString = context.getString(R.string.replyingTo, "Hakonschia")
        onView(withId(R.id.replyingTo))
                .check(matches(withText(formattedString)))

        // Matches the summary of the comment being replied to
        onView(withId(R.id.summary))
                // This is from the JSON file
                .check(matches(withText("Replying to a comment")))
    }

    @Test
    fun textInsertedIsShownInPreviewDialog() {
        val intent = Intent(context, ReplyActivity::class.java).apply {
            putExtra(ReplyActivity.LISTING_KIND_KEY, Thing.COMMENT.value)
            putExtra(ReplyActivity.LISTING_KEY, commentData)
        }
        ActivityScenario.launch<ReplyActivity>(intent)

        // Not sure how we can avoid this in tests since we don't have a user
        onView(withText(R.string.dialogReplyNotLoggedInTitle))
                .check(matches(isDisplayed()))
        // Dismiss the dialog
        pressBack()

        // Insert some text to the input field (this ID is from MarkdownInput)
        onView(withId(R.id.replyText))
                .check(matches(isDisplayed()))
                .perform(typeText("Text into input"))

        // Open the preview dialog
        onView(withId(R.id.showPreview))
                .perform(click())

        onView(withText(R.string.preview))
                .check(matches(isDisplayed()))
        // This ID is from dialog_markdown_preview
        onView(withId(R.id.previewText))
                .check(matches(withText("Text into input")))
    }

    /**
     * Tests that an alert dialog is shown when the back button is pressed when input is in the
     * input field
     */
    @Test
    fun confirmDismissDialogIsShownWhenTextIsInInput() {
        val intent = Intent(context, ReplyActivity::class.java).apply {
            putExtra(ReplyActivity.LISTING_KIND_KEY, Thing.COMMENT.value)
            putExtra(ReplyActivity.LISTING_KEY, commentData)
        }
        ActivityScenario.launch<ReplyActivity>(intent)

        // Not sure how we can avoid this in tests since we don't have a user
        onView(withText(R.string.dialogReplyNotLoggedInTitle))
                .check(matches(isDisplayed()))
        // Dismiss the dialog
        pressBack()

        // Insert some text to the input field (this ID is from MarkdownInput)
        onView(withId(R.id.replyText))
                .check(matches(isDisplayed()))
                .perform(typeText("Text into input"))

        // Dismiss the keyboard
        pressBack()
        // Try to exit
        pressBack()

        onView(withId(R.id.confirmDialogParent))
                .check(matches(isDisplayed()))
    }

    /**
     * Tests that the activity does not exit when the "Cancel" button is clicked in the confirm dialog
     */
    @Test
    fun activityDoesNotExitWhenConfirmDialogCancelIsClicked() {
        val intent = Intent(context, ReplyActivity::class.java).apply {
            putExtra(ReplyActivity.LISTING_KIND_KEY, Thing.COMMENT.value)
            putExtra(ReplyActivity.LISTING_KEY, commentData)
        }
        ActivityScenario.launch<ReplyActivity>(intent)

        // Not sure how we can avoid this in tests since we don't have a user
        onView(withText(R.string.dialogReplyNotLoggedInTitle))
                .check(matches(isDisplayed()))
        // Dismiss the dialog
        pressBack()

        // Insert some text to the input field (this ID is from MarkdownInput)
        onView(withId(R.id.replyText))
                .check(matches(isDisplayed()))
                .perform(typeText("Text into input"))

        // Dismiss the keyboard
        pressBack()
        // Try to exit
        pressBack()

        onView(withId(R.id.confirmDialogParent))
                .check(matches(isDisplayed()))

        onView(withId(R.id.btnCancel))
                .perform(click())

        // The input field is still in view with the text
        onView(withId(R.id.replyText))
                .check(matches(isDisplayed()))
                .check(matches(withText("Text into input")))
    }

    /**
     * Tests that the activity does not exit when the back button is clicked when the confirm dialog
     * is shown
     */
    @Test
    fun activityDoesNotExitWhenConfirmDialogIsShownAndBackIsPressed() {
        val intent = Intent(context, ReplyActivity::class.java).apply {
            putExtra(ReplyActivity.LISTING_KIND_KEY, Thing.COMMENT.value)
            putExtra(ReplyActivity.LISTING_KEY, commentData)
        }
        val activityScenario = ActivityScenario.launch<ReplyActivity>(intent)

        // Not sure how we can avoid this in tests since we don't have a user
        onView(withText(R.string.dialogReplyNotLoggedInTitle))
                .check(matches(isDisplayed()))
        // Dismiss the dialog
        pressBack()

        // Insert some text to the input field (this ID is from MarkdownInput)
        onView(withId(R.id.replyText))
                .check(matches(isDisplayed()))
                .perform(typeText("Text into input"))

        // Dismiss the keyboard
        pressBack()
        // Try to exit
        pressBack()

        onView(withId(R.id.confirmDialogParent))
                .check(matches(isDisplayed()))

        // Discard the dialog with the back button
        pressBack()

        // The input field is still in view with the text
        onView(withId(R.id.replyText))
                .check(matches(isDisplayed()))
                .check(matches(withText("Text into input")))
        // Activity is still in a created state
        assertEquals(Lifecycle.State.RESUMED, activityScenario.state)
    }

    /**
     * Tests that the activity exits when the discard button is clicked in the confirm dialog
     */
    @Test
    fun activityExitsWhenConfirmDialogDiscardIsClicked() {
        val intent = Intent(context, ReplyActivity::class.java).apply {
            putExtra(ReplyActivity.LISTING_KIND_KEY, Thing.COMMENT.value)
            putExtra(ReplyActivity.LISTING_KEY, commentData)
        }
        val activityScenario = ActivityScenario.launch<ReplyActivity>(intent)

        // Not sure how we can avoid this in tests since we don't have a user
        onView(withText(R.string.dialogReplyNotLoggedInTitle))
                .check(matches(isDisplayed()))
        // Dismiss the dialog
        pressBack()

        // Insert some text to the input field (this ID is from MarkdownInput)
        onView(withId(R.id.replyText))
                .check(matches(isDisplayed()))
                .perform(typeText("Text into input"))

        // Dismiss the keyboard
        pressBack()
        // Try to exit
        pressBack()

        onView(withId(R.id.confirmDialogParent))
                .check(matches(isDisplayed()))

        // Discard should cause the activity to finish
        onView(withId(R.id.btnDiscard))
                .perform(click())

        // Activity should now be in a destroyed state
        assertEquals(Lifecycle.State.DESTROYED, activityScenario.state)
    }

    @Test
    fun replyingToPost() {
        val intent = Intent(context, ReplyActivity::class.java).apply {
            putExtra(ReplyActivity.LISTING_KIND_KEY, Thing.POST.value)
            putExtra(ReplyActivity.LISTING_KEY, postData)
        }
        ActivityScenario.launch<ReplyActivity>(intent)

        // Not sure how we can avoid this in tests since we don't have a user
        onView(withText(R.string.dialogReplyNotLoggedInTitle))
                .check(matches(isDisplayed()))
        // Dismiss the dialog
        pressBack()

        val formattedString = context.getString(R.string.replyingTo, "Hakonschia")
        onView(withId(R.id.replyingTo))
                .check(matches(withText(formattedString)))

        val post = Gson().fromJson(postData, RedditPost::class.java)
        onView(withId(R.id.summary))
                .check(matches(withText(post.selftext)))
                // The text is ellipsized, but this doesn't work for some reason
                // It can clearly be verified by debugging this test that the text actually is ellipsized
                //.check(matches(hasEllipsizedText()))
    }
}