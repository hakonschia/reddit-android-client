package com.example.hakonsreader.activities

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hakonsreader.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class SendPrivateMessageActivityTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext


    /**
     * Tests that the input field for the recipient is already set when passed to the activity
     */
    @Test
    fun recipientIsSetWhenPassed() {
        val intent = Intent(context, SendPrivateMessageActivity::class.java).apply {
            putExtra(SendPrivateMessageActivity.EXTRAS_RECIPIENT, "AutoModerator")
        }
        ActivityScenario.launch<SendPrivateMessageActivity>(intent)

        onView(withId(R.id.recipientInput))
                .check(matches(withText("AutoModerator")))
    }

    /**
     * Tests that the input field for the recipient is already set when passed to the activity,
     * when the recipient is a subreddit
     */
    @Test
    fun recipientIsSetWhenPassedWhenRecipientIsSubreddit() {
        val intent = Intent(context, SendPrivateMessageActivity::class.java).apply {
            putExtra(SendPrivateMessageActivity.EXTRAS_RECIPIENT, "/r/hakonschia")
        }
        ActivityScenario.launch<SendPrivateMessageActivity>(intent)

        onView(withId(R.id.recipientInput))
                .check(matches(withText("/r/hakonschia")))
    }

    /**
     * Tests that the input field for the subject has focus automatically when a recipient
     * is passed to the activity
     */
    @Test
    fun subjectInputHasFocusWhenRecipientIsSetWhenPassed() {
        val intent = Intent(context, SendPrivateMessageActivity::class.java).apply {
            putExtra(SendPrivateMessageActivity.EXTRAS_RECIPIENT, "AutoModerator")
        }
        ActivityScenario.launch<SendPrivateMessageActivity>(intent)

        onView(withId(R.id.recipientInput))
                .check(matches(withText("AutoModerator")))

        onView(withId(R.id.subjectInput))
                .check(matches(hasFocus()))
    }

    @Test
    fun subjectIsSetWhenPassed() {
        val intent = Intent(context, SendPrivateMessageActivity::class.java).apply {
            putExtra(SendPrivateMessageActivity.EXTRAS_SUBJECT, "This is a message")
        }
        ActivityScenario.launch<SendPrivateMessageActivity>(intent)

        onView(withId(R.id.subjectInput))
                .check(matches(withText("This is a message")))
    }

    /**
     * Tests that the input field for the recipient has focus automatically when a subject is passed
     * but a recipient is not
     */
    @Test
    fun recipientInputHasFocusWhenSubjectIsPassedButNotRecipient() {
        val intent = Intent(context, SendPrivateMessageActivity::class.java).apply {
            putExtra(SendPrivateMessageActivity.EXTRAS_SUBJECT, "This is a message")
        }
        ActivityScenario.launch<SendPrivateMessageActivity>(intent)

        onView(withId(R.id.subjectInput))
                .check(matches(withText("This is a message")))

        // This is a view inside MarkdownInput
        onView(withId(R.id.recipientInput))
                .check(matches(hasFocus()))
    }

    /**
     * Tests that the input field for the message has focus automatically when a recipient and subject
     * is passed to the activity
     */
    @Test
    fun messageInputHasFocusWhenRecipientAndSubjectIsSetWhenPassed() {
        val intent = Intent(context, SendPrivateMessageActivity::class.java).apply {
            putExtra(SendPrivateMessageActivity.EXTRAS_RECIPIENT, "AutoModerator")
            putExtra(SendPrivateMessageActivity.EXTRAS_SUBJECT, "This is a message")
        }
        ActivityScenario.launch<SendPrivateMessageActivity>(intent)

        onView(withId(R.id.recipientInput))
                .check(matches(withText("AutoModerator")))

        onView(withId(R.id.subjectInput))
                .check(matches(withText("This is a message")))

        // This is a view inside MarkdownInput
        onView(withId(R.id.replyText))
                .check(matches(hasFocus()))
    }
}