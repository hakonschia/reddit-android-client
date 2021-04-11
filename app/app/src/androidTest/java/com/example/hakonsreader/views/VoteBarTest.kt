package com.example.hakonsreader.views

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hakonsreader.*
import com.example.hakonsreader.api.model.RedditComment
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.interfaces.VoteableListing
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import com.example.hakonsreader.di.TestApiModule
import org.hamcrest.Matchers.not


/**
 * Tests for [VoteBar]
 */
@HiltAndroidTest
class VoteBarTest {
    lateinit var context: Context
    lateinit var voteBar: VoteBar

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext

        val activity = ActivityScenario.launch<HiltTestActivity>(Intent(context, HiltTestActivity::class.java))
        activity.onActivity {
            voteBar = it.addOnlyOneView(VoteBar::class.java)
        }
    }


    /**
     * Tests that a [VoteBar] has matching text according to a score given, and that the text color
     * matches the liked status given
     */
    @Test
    fun textIsSetAndTextColorIsSet() {
        voteBar.listing = RedditPost().apply {
            score = 0
            liked = null
        }

        // No score and no like, should be 0 score and noVote color
        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.text_color)))
                .check(matches(tickerViewHasText("0")))
        onView(withId(R.id.upvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))
        onView(withId(R.id.downvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))

        voteBar.listing = RedditPost().apply {
            score = 50
            liked = null
        }
        // 50 score and no like, should be 50 score and noVote color
        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.text_color)))
                .check(matches(tickerViewHasText("50")))
        onView(withId(R.id.upvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))
        onView(withId(R.id.downvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))

        voteBar.listing = RedditPost().apply {
            score = 60
            liked = true
        }
        // 50 score and liked, should be 50 score and upvote color
        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.upvoted)))
                .check(matches(tickerViewHasText("60")))
        onView(withId(R.id.upvote))
                .check(matches(imageHasColorInColorFilter(R.color.upvoted)))
        // The downvote should not be colored when upvote is
        onView(withId(R.id.downvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))

        voteBar.listing = RedditPost().apply {
            score = 70
            liked = false
        }
        // 50 score and liked, should be 50 score and downvote color
        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.downvoted)))
                .check(matches(tickerViewHasText("70")))
        onView(withId(R.id.downvote))
                .check(matches(imageHasColorInColorFilter(R.color.downvoted)))
        onView(withId(R.id.upvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))


        // RedditComment is also a VoteableListing, should be no different
        voteBar.listing = RedditComment().apply {
            score = 80
            liked = false
        }
        // 50 score and liked, should be 50 score and downvote color
        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.downvoted)))
                .check(matches(tickerViewHasText("80")))
        onView(withId(R.id.downvote))
                .check(matches(imageHasColorInColorFilter(R.color.downvoted)))
        onView(withId(R.id.upvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))
    }

    /**
     * Tests that a vote bar hides the score of a listing when [VoteableListing.isScoreHidden] is true
     */
    @Test
    fun scoreIsHiddenWhenListingSaysTo() {
        voteBar.listing = RedditPost().apply {
            score = 100
            liked = null
            isScoreHidden = true
        }

        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.text_color)))
                .check(matches(tickerViewHasText(context.getString(R.string.scoreHidden))))

        // Upvote the listing. When the score is hidden the hidden score icon should also be colored
        voteBar.listing = RedditPost().apply {
            score = 100
            liked = true
            isScoreHidden = true
        }

        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.upvoted)))
                .check(matches(tickerViewHasText(context.getString(R.string.scoreHidden))))
    }

    /**
     * Tests that a vote bar hides the score of a listing when the vote bar has been asked manually
     * to hide the score with [VoteBar.hideScore]
     */
    @Test
    fun scoreIsHiddenWhenManuallySetToHide() {
        voteBar.hideScore = true
        voteBar.listing = RedditPost().apply {
            score = 100
            liked = null
            isScoreHidden = false
        }

        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.text_color)))
                .check(matches(tickerViewHasText(context.getString(R.string.scoreHidden))))

        voteBar.listing = RedditPost().apply {
            score = 100
            liked = false
        }

        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.downvoted)))
                .check(matches(tickerViewHasText(context.getString(R.string.scoreHidden))))
    }


    /**
     * Tests that going from no vote to an upvote updates the upvote button color and updates the
     * score by adding 1 point
     */
    @Test
    fun noVoteToUpvote() {
        // Go from no vote to upvote
        voteBar.listing = RedditPost().apply {
            score = 100
            liked = null
        }

        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.text_color)))
                .check(matches(tickerViewHasText("100")))

        onView(withId(R.id.upvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))
                .perform(click())
                .check(matches(imageHasColorInColorFilter(R.color.upvoted)))

        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.upvoted)))
                .check(matches(tickerViewHasText("101")))
    }

    /**
     * Tests that going from no vote to an downvote updates the downvote button color and updates the
     * score by removing 1 point
     */
    @Test
    fun novoteToDownvote() {
        voteBar.listing = RedditPost().apply {
            score = 100
            liked = null
        }

        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.text_color)))
                .check(matches(tickerViewHasText("100")))

        onView(withId(R.id.downvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))
                .perform(click())
                .check(matches(imageHasColorInColorFilter(R.color.downvoted)))

        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.downvoted)))
                .check(matches(tickerViewHasText("99")))
    }

    /**
     * Tests that going from a downvote to an upvote updates both vote button colors and updates the
     * score by removing 2 points
     */
    @Test
    fun upvoteToDownvote() {
        voteBar.listing = RedditPost().apply {
            score = 100
            liked = true
        }

        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.upvoted)))
                .check(matches(tickerViewHasText("100")))

        onView(withId(R.id.upvote))
                .check(matches(imageHasColorInColorFilter(R.color.upvoted)))

        onView(withId(R.id.downvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))
                .perform(click())
                .check(matches(imageHasColorInColorFilter(R.color.downvoted)))

        onView(withId(R.id.upvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))

        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.downvoted)))
                .check(matches(tickerViewHasText("98")))
    }

    /**
     * Tests that going from a downvote to an upvote updates both vote button colors and updates the
     * score by adding 2 points
     */
    @Test
    fun downvoteToUpvote() {
        // Go from downvote to upvote. New score should be 102
        voteBar.listing = RedditPost().apply {
            score = 100
            liked = false
        }

        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.downvoted)))
                .check(matches(tickerViewHasText("100")))

        onView(withId(R.id.downvote))
                .check(matches(imageHasColorInColorFilter(R.color.downvoted)))

        onView(withId(R.id.upvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))
                .perform(click())
                .check(matches(imageHasColorInColorFilter(R.color.upvoted)))

        onView(withId(R.id.downvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))

        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.upvoted)))
                .check(matches(tickerViewHasText("102")))
    }

    /**
     * Tests that the vote buttons are temporarily disabled right after being clicked to ensure
     * they cannot be spammed
     */
    @Test
    fun voteButtonsAreDisabledRightAfterAVote() {
        voteBar.listing = RedditPost().apply {
            score = 100
            liked = null
        }

        onView(withId(R.id.upvote))
                .check(matches(isEnabled()))
                .perform(click())
                .check(matches(not(isEnabled())))

        onView(isRoot())
                // This is a bit longer than the actual delay just to be sure it everything will has been called
                .perform(waitFor(500L))

        // Should noe be enabled again
        onView(withId(R.id.upvote))
                .check(matches(isEnabled()))

        // Test with downvote as well
        onView(withId(R.id.downvote))
                .check(matches(isEnabled()))
                .perform(click())
                .check(matches(not(isEnabled())))

        onView(isRoot())
                .perform(waitFor(500L))

        // Should noe be enabled again
        onView(withId(R.id.downvote))
                .check(matches(isEnabled()))
    }

    /**
     * Tests that when an error occurs when performing the API request the color and score does not
     * change
     */
    @Test
    fun errorDoesNotChangeColorAndScore() {
        voteBar.listing = RedditPost().apply {
            score = 100
            liked = null
            // Passing this as an ID causes the fake API to send back an error
            id = TestApiModule.VOTE_FAIL
        }

        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.text_color)))
                .check(matches(tickerViewHasText("100")))

        onView(withId(R.id.upvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))
                .perform(click())
                // Still noVote color (not upvote)
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))

        // Still 100 score
        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.text_color)))
                .check(matches(tickerViewHasText("100")))


        onView(withId(R.id.downvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))
                .perform(click())
                // Still noVote color (not downvote)
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))

        // Still 100 score
        onView(withId(R.id.score))
                .check(matches(hasTextColorWithTicker(R.color.text_color)))
                .check(matches(tickerViewHasText("100")))
    }


    /**
     * Tests that a snackbar is shown if a listing that is archived tries to be voted on.
     * Tests that a different message appears based on if the listing is a comment/post
     */
    @Test
    fun snackbarIsShownWhenListingIsArchived() {
        voteBar.listing = RedditPost().apply {
            score = 100
            liked = null
            isArchived = true
        }

        onView(withId(R.id.score))
                .check(matches(tickerViewHasText("100")))
        onView(withId(R.id.upvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))
                .perform(click())

        onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.postHasBeenArchivedVote)))

        // Score and vote color not changed
        onView(withId(R.id.upvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))
        onView(withId(R.id.score))
                .check(matches(tickerViewHasText("100")))

        // This is pretty bad, but to ensure the previous snackbar has been dismissed
        // This is also somewhat relies on knowing that the snackbar has LENGTH_SHORT (which is 1500ms)
        onView(isRoot())
                .perform(waitFor(2000))

        onView(withId(R.id.downvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))
        onView(withId(R.id.score))
                .check(matches(tickerViewHasText("100")))

        onView(withId(R.id.downvote))
                .perform(click())

        onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.postHasBeenArchivedVote)))

        onView(withId(R.id.downvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))
        onView(withId(R.id.score))
                .check(matches(tickerViewHasText("100")))

        onView(isRoot())
                .perform(waitFor(2000))


        // Comments should have a snackbar with a different message
        voteBar.listing = RedditComment().apply {
            score = 500
            liked = null
            isArchived = true
        }

        onView(withId(R.id.score))
                .check(matches(tickerViewHasText("500")))
        onView(withId(R.id.upvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))
                .perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.commentHasBeenArchivedVote)))

        // This is pretty bad, but to ensure the previous snackbar has been dismissed
        // This is also somewhat relies on knowing that the snackbar has LENGTH_SHORT (which is 1500ms)
        onView(isRoot())
                .perform(waitFor(2000))

        onView(withId(R.id.score))
                .check(matches(tickerViewHasText("500")))
        onView(withId(R.id.downvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))
                .perform(click())

        onView(withId(com.google.android.material.R.id.snackbar_text))
                .check(matches(isDisplayed()))
                .check(matches(withText(R.string.commentHasBeenArchivedVote)))

        onView(withId(R.id.downvote))
                .check(matches(imageHasColorInColorFilter(R.color.noVote)))
        onView(withId(R.id.score))
                .check(matches(tickerViewHasText("500")))
    }

    /**
     * Tests that a NPE is not thrown when the vote buttons are clicked without a listing
     */
    @Test
    fun appDoesNotCrashWhenNoListingIsSet() {
        onView(withId(R.id.upvote))
                .perform(click())
        onView(withId(R.id.downvote))
                .perform(click())
    }
}
