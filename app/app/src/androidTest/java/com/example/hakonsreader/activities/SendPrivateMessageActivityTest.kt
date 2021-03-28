package com.example.hakonsreader.activities

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hakonsreader.R
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.persistence.RedditDatabase
import com.example.hakonsreader.api.persistence.RedditUserInfoDatabase
import com.example.hakonsreader.constants.SharedPreferencesConstants
import com.example.hakonsreader.misc.Settings
import com.example.hakonsreader.misc.SharedPreferencesManager
import com.example.hakonsreader.states.AppState
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
class SendPrivateMessageActivityTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var api: RedditApi

    @Inject
    lateinit var database: RedditDatabase

    @Inject
    lateinit var userInfoDatabase: RedditUserInfoDatabase

    @Before
    fun init() {
        hiltRule.inject()

        // This has to set before AppState.init()
        SharedPreferencesManager.create(InstrumentationRegistry.getInstrumentation().targetContext
                .getSharedPreferences(SharedPreferencesConstants.PREFS_NAME, Application.MODE_PRIVATE)
        )
        AppState.init(api, database, userInfoDatabase)
        Settings.init(InstrumentationRegistry.getInstrumentation().targetContext)
    }


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
     * Tests that the subject is set on the input field when passed
     */
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
     * Tests that the message input field is set automatically when passed
     */
    @Test
    fun messageIsSetWhenPassed() {
        val intent = Intent(context, SendPrivateMessageActivity::class.java).apply {
            putExtra(SendPrivateMessageActivity.EXTRAS_MESSAGE, "This is the content of the message")
        }
        ActivityScenario.launch<SendPrivateMessageActivity>(intent)

        // This is a view inside MarkdownInput
        onView(withId(R.id.replyText))
                .check(matches(withText("This is the content of the message")))
    }

    /**
     * Tests that the recipient input has focus by default
     */
    @Test
    fun recipientHasFocusWhenNothingIsPassed() {
        ActivityScenario.launch(SendPrivateMessageActivity::class.java)

        onView(withId(R.id.recipientInput))
                .check(matches(hasFocus()))
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