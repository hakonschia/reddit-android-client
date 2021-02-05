package com.example.hakonsreader.misc

import android.content.Context
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.example.hakonsreader.activites.*
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Test class for the [createIntent] function
 */
class CreateIntentTest {
    lateinit var instrumentationContext: Context

    @Before
    fun setup() {
        instrumentationContext = InstrumentationRegistry.getInstrumentation().targetContext
    }

    /**
     * Asserts that an intent is for a given class
     *
     * @param intent The intent to verify
     * @param clazz The class the intent should be for
     */
    private fun <T>assertIntentIsForClass(intent: Intent, clazz: Class<T>) {
        val actual = intent.component?.className
        val expected = clazz.name
        assertEquals(expected, actual)
    }

    /**
     * Asserts that an intent has a given extra, and that the extra matches a given value
     *
     * @param intent The intent to check
     * @param name The name of the extra in the intent
     * @param value The value of the extra
     */
    private fun assertIntentHasExtras(intent: Intent, name: String, value: Any) {
        val hasExtra = intent.hasExtra(name)
        assertEquals("Extra with name '$name' not found", true, hasExtra)

        val valueClass = value::class.java
        // We have already asserted that the value exists, so it cannot be nulled
        val actualValue = intent.extras?.get(name)!!
        assertEquals(true, actualValue::class.java == valueClass)
        assertEquals(value, actualValue)
    }

    /**
     * This tests that [createIntent] creates intents that should resolve to [MainActivity]
     */
    @Test
    fun dispatchesToMainActivity() {
        val clazz = MainActivity::class.java
        var intent: Intent = createIntent("https://reddit.com/", instrumentationContext)
        assertIntentIsForClass(intent, clazz)

        intent = createIntent("https://www.reddit.com", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        intent = createIntent("https://old.reddit.com", instrumentationContext)
        assertIntentIsForClass(intent, clazz)

        intent = createIntent("https://redd.it", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        intent = createIntent("https://redd.it/", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        intent = createIntent("https://www.redd.it", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        intent = createIntent("https://www.redd.it/", instrumentationContext)
        assertIntentIsForClass(intent, clazz)

        // Typically, this should resolve to a SubredditActivity, but "popular" and "all"
        // are both in MainActivity as they are "special" types of subreddits
        intent = createIntent("https://reddit.com/r/popular", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, MainActivity.START_SUBREDDIT, "popular")

        intent = createIntent("https://reddit.com/r/all", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, MainActivity.START_SUBREDDIT, "all")
    }

    /**
     * Tests that [createIntent] dispatches links for subreddits (reddit.com/r/hakonschia) and that
     * parameters, such as sorting, is passed correctly
     */
    @Test
    fun dispatchesToSubredditActivity() {
        val clazz = SubredditActivity::class.java
        var intent: Intent = createIntent("https://reddit.com/r/hakonschia", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, SubredditActivity.SUBREDDIT_KEY, "hakonschia")

        intent = createIntent("https://reddit.com/r/hakonschia/top/?t=year", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, SubredditActivity.SUBREDDIT_KEY, "hakonschia")
        assertIntentHasExtras(intent, SubredditActivity.SORT, SortingMethods.TOP.value)
        assertIntentHasExtras(intent, SubredditActivity.TIME_SORT, PostTimeSort.YEAR.value)

        intent = createIntent("https://www.reddit.com/r/hakonschia/top/?t=year", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, SubredditActivity.SUBREDDIT_KEY, "hakonschia")
        assertIntentHasExtras(intent, SubredditActivity.SORT, SortingMethods.TOP.value)
        assertIntentHasExtras(intent, SubredditActivity.TIME_SORT, PostTimeSort.YEAR.value)

        // The reddit domain should be appended and dispatched correctly
        intent = createIntent("r/hakonschia/new", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, SubredditActivity.SUBREDDIT_KEY, "hakonschia")
        assertIntentHasExtras(intent, SubredditActivity.SORT, SortingMethods.NEW.value)
        // The first "/" should be added only as needed
        intent = createIntent("/r/hakonschia/new", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, SubredditActivity.SUBREDDIT_KEY, "hakonschia")
        assertIntentHasExtras(intent, SubredditActivity.SORT, SortingMethods.NEW.value)

        // The reddit domain should be appended and dispatched correctly
        intent = createIntent("r/hakonschia/about/rules", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, SubredditActivity.SUBREDDIT_KEY, "hakonschia")
        assertIntentHasExtras(intent, SubredditActivity.SHOW_RULES, true)

        intent = createIntent("r/GlobalOffensive", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        // The subreddit will be lowercased
        assertIntentHasExtras(intent, SubredditActivity.SUBREDDIT_KEY, "globaloffensive")
    }

    /**
     * Tests that [createIntent] dispatches links for users (reddit.com/u/hakonschia)
     */
    @Test
    fun dispatchesToProfileActivity() {
        val clazz = ProfileActivity::class.java
        var intent: Intent = createIntent("https://reddit.com/u/hakonschia", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, ProfileActivity.USERNAME_KEY, "hakonschia")

        // Either /u/ or /user/
        intent = createIntent("https://reddit.com/user/hakonschia", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, ProfileActivity.USERNAME_KEY, "hakonschia")

        // Either /u/ or /user/
        intent = createIntent("https://old.reddit.com/user/hakonschia/", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, ProfileActivity.USERNAME_KEY, "hakonschia")
    }

    /**
     * Tests that [createIntent] dispatches links for posts (reddit.com/comments/gh4uy3)
     *
     * Several URL types should create this intent:
     * * redd.it/kx3hiz
     * * reddit.com/comments/kx3hiz
     * * reddit.com/comments/kx3hiz/stop_wasting_my_moonlight/
     * * reddit.com/r/hakonschia/comments/kx3hiz/stop_wasting_my_moonlight/
     * * reddit.com/r/hakonschia/comments/kx3hiz/stop_wasting_my_moonlight/gj8cot6? <- To a specific comment chain
     */
    @Test
    fun dispatchesToPostActivity() {
        val postId = "kx3hiz"
        val clazz = PostActivity::class.java
        var intent: Intent = createIntent("https://redd.it/kx3hiz", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, PostActivity.POST_ID_KEY, postId)
        intent = createIntent("https://redd.it/kx3hiz/", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, PostActivity.POST_ID_KEY, postId)

        intent = createIntent("https://www.reddit.com/r/hakonschia/comments/kx3hiz/stop_wasting_my_moonlight/", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, PostActivity.POST_ID_KEY, postId)

        intent = createIntent("https://www.reddit.com/r/hakonschia/comments/kx3hiz/stop_wasting_my_moonlight/gj8cot6", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, PostActivity.POST_ID_KEY, postId)
        assertIntentHasExtras(intent, PostActivity.COMMENT_ID_CHAIN, "gj8cot6")

        intent = createIntent("/r/hakonschia/comments/kx3hiz/stop_wasting_my_moonlight/", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, PostActivity.POST_ID_KEY, postId)

        intent = createIntent("https://www.reddit.com/comments/kx3hiz", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, PostActivity.POST_ID_KEY, postId)

        intent = createIntent("comments/kx3hiz", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, PostActivity.POST_ID_KEY, postId)
    }

    /**
     * Tests that [createIntent] dispatches links for sending private messages (reddit.com/message/compose).
     * This will also test that the given query parameters are passed
     */
    @Test
    fun dispatchesToSendPrivateMessageActivity() {
        val clazz = SendPrivateMessageActivity::class.java
        var intent: Intent = createIntent("https://reddit.com/message/compose", instrumentationContext)
        assertIntentIsForClass(intent, clazz)

        intent = createIntent("https://reddit.com/message/compose?to=hakonschia", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, SendPrivateMessageActivity.EXTRAS_RECIPIENT, "hakonschia")

        // Messages to mods of a subreddit
        intent = createIntent("https://reddit.com/message/compose?to=/r/hakonschia", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, SendPrivateMessageActivity.EXTRAS_RECIPIENT, "/r/hakonschia")

        intent = createIntent("https://reddit.com/message/compose?to=hakonschia&subject=test", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, SendPrivateMessageActivity.EXTRAS_RECIPIENT, "hakonschia")
        assertIntentHasExtras(intent, SendPrivateMessageActivity.EXTRAS_SUBJECT, "test")

        intent = createIntent("https://reddit.com/message/compose?to=hakonschia&subject=subject with spaces", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, SendPrivateMessageActivity.EXTRAS_RECIPIENT, "hakonschia")
        assertIntentHasExtras(intent, SendPrivateMessageActivity.EXTRAS_SUBJECT, "subject with spaces")

        intent = createIntent("https://reddit.com/message/compose?to=hakonschia&subject=hello&message=The post should be removed", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, SendPrivateMessageActivity.EXTRAS_RECIPIENT, "hakonschia")
        assertIntentHasExtras(intent, SendPrivateMessageActivity.EXTRAS_SUBJECT, "hello")
        assertIntentHasExtras(intent, SendPrivateMessageActivity.EXTRAS_MESSAGE, "The post should be removed")
    }

    /**
     * Tests that [createIntent] dispatches image links to [ImageActivity], so that image links clicked
     * are opened inside the app
     */
    @Test
    fun dispatchesToImageActivity() {
        val clazz = ImageActivity::class.java
        var intent: Intent = createIntent("https://i.imgur.com/0Uytu2X.jpeg", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, ImageActivity.IMAGE_URL, "https://i.imgur.com/0Uytu2X.jpeg")

        intent = createIntent("https://i.imgur.com/0Uytu2X.jpg", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, ImageActivity.IMAGE_URL, "https://i.imgur.com/0Uytu2X.jpg")

        intent = createIntent("https://i.imgur.com/0Uytu2X.png", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, ImageActivity.IMAGE_URL, "https://i.imgur.com/0Uytu2X.png")

        // No file extension, but it has an image format as a query parameter
        intent = createIntent("https://pbs.twimg.com/media/Es_qtWVXEAMKEBd?format=jpg&name=large", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, ImageActivity.IMAGE_URL, "https://pbs.twimg.com/media/Es_qtWVXEAMKEBd?format=jpg&name=large")
    }

    /**
     * Tests that [createIntent] dispatches image links to [dispatchesToVideoYoutubeActivity], so that
     * YouTube links clicked are opened inside the app
     */
    @Test
    fun dispatchesToVideoYoutubeActivity() {
        val clazz = VideoYoutubeActivity::class.java
        var intent: Intent = createIntent("https://www.youtube.com/watch?v=dQw4w9WgXcQ", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, VideoYoutubeActivity.VIDEO_ID, "dQw4w9WgXcQ")

        intent = createIntent("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=86", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, VideoYoutubeActivity.VIDEO_ID, "dQw4w9WgXcQ")
        assertIntentHasExtras(intent, VideoYoutubeActivity.TIMESTAMP, 86f)

        intent = createIntent("https://youtu.be/dQw4w9WgXcQ", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, VideoYoutubeActivity.VIDEO_ID, "dQw4w9WgXcQ")

        intent = createIntent("https://youtu.be/dQw4w9WgXcQ?t=86", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, VideoYoutubeActivity.VIDEO_ID, "dQw4w9WgXcQ")
        assertIntentHasExtras(intent, VideoYoutubeActivity.TIMESTAMP, 86f)
    }

    /**
     * Tests that [createIntent] dispatches links to [WebViewActivity] as a last resort, so that
     * links not handled explicitly are still shown
     */
    @Test
    fun dispatchesToWebViewActivity() {
        val clazz = WebViewActivity::class.java
        var intent: Intent = createIntent("https://www.nrk.no", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, WebViewActivity.URL, "https://www.nrk.no")

        // We don't (currently) handle this path, so it should not resolve to our app
        intent = createIntent("https://www.reddit.com/r/hakonschia/wiki", instrumentationContext)
        assertIntentIsForClass(intent, clazz)
        assertIntentHasExtras(intent, WebViewActivity.URL, "https://www.reddit.com/r/hakonschia/wiki")
    }
}