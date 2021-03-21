package com.example.hakonsreader.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.enums.FlairType
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.model.Submission
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.model.flairs.RedditFlair
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.databinding.ActivitySubmitBinding
import com.example.hakonsreader.databinding.SubmissionCrosspostBinding
import com.example.hakonsreader.databinding.SubmissionLinkBinding
import com.example.hakonsreader.databinding.SubmissionTextBinding
import com.example.hakonsreader.dialogadapters.RedditFlairAdapter
import com.example.hakonsreader.misc.handleGenericResponseErrors
import com.example.hakonsreader.viewmodels.SubredditFlairsViewModel
import com.example.hakonsreader.viewmodels.SubredditViewModel
import com.example.hakonsreader.viewmodels.factories.SubredditFactory
import com.example.hakonsreader.viewmodels.factories.SubredditFlairsFactory
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Activity for submitting a post to a subreddit. The ID of the newly created post (if one was created)
 * will be passed with the key [RESULT_POST_ID]
 */
class SubmitActivity : BaseActivity() {

    companion object {
        private const val TAG = "SubmitActivity"

        /**
         * The key used to tell the name of the subreddit being submitted to
         */
        const val SUBREDDIT_KEY = "submittingToSubredditName"

        /**
         * The key used in activity results to tell the ID of the post submitted
         */
        const val RESULT_POST_ID = "resultPostId"
    }

    private val api = App.get().api
    private val database = App.get().database

    private lateinit var binding: ActivitySubmitBinding

    private lateinit var subreddit: Subreddit
    private lateinit var subredditName: String
    private var subredditViewModel: SubredditViewModel? = null

    private var flairsViewModel: SubredditFlairsViewModel? = null

    private val submissionFragments = ArrayList<Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBinding()

        subredditName = intent?.extras?.getString(SUBREDDIT_KEY) ?: return
        binding.subredditName = subredditName

        setupSubredditViewModel(subredditName)

        // Setup with all tabs initially until we know which submissions are supported on the subreddit
        setupTabs()
        // Force updates if data saving isn't enabled
        setupFlairsViewModel(subredditName)
    }

    private fun setupBinding() {
        binding = ActivitySubmitBinding.inflate(LayoutInflater.from(this))
        with (binding) {
            setContentView(root)

            binding.showPreview.setOnClickListener {
                val textFragment = submissionFragments[0] as SubmissionTextFragment
                textFragment.binding?.markdownInput?.showPreviewInPopupDialog()
            }

            submissionTypes.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    showPreview.visibility = if (submissionFragments[position] is SubmissionTextFragment) {
                        VISIBLE
                    } else {
                        // Use INVISIBLE instead of GONE since the "Submit post" button relies on the position of the preview
                        INVISIBLE
                    }
                }
            })

            submitPost.setOnClickListener {
                if (title.text?.length == 0) {
                    // Too short
                    Snackbar.make(root, R.string.submittingNoTitle, Snackbar.LENGTH_LONG).show()

                    return@setOnClickListener
                } else if (title.text?.length!! > resources.getInteger(R.integer.submissionTitleMaxLength)) {
                    // Too long
                    Snackbar.make(binding.root, R.string.submittingTitleTooLong, Snackbar.LENGTH_LONG).show()

                    return@setOnClickListener
                }

                // Check which page is active, submit based on that
                when (val fragment = submissionFragments[submissionTypes.currentItem]) {
                    is SubmissionTextFragment -> submitText(fragment)
                    is SubmissionLinkFragment -> submitLink(fragment)
                    is SubmissionCrosspostFragment -> submitCrosspost(fragment)
                }
            }
        }
    }

    private fun setupSubredditViewModel(subredditName: String) {
        subredditViewModel = ViewModelProvider(this, SubredditFactory(
                subredditName,
                database.subreddits(),
                database.posts()
        )).get(SubredditViewModel::class.java).apply {
            subreddit.observe(this@SubmitActivity) {
                // If this is null then it should probably be reflected on the subreddit field in the fragment?
                // Probably won't ever happen though
                if (it == null) {
                    return@observe
                }

                this@SubmitActivity.subreddit = it
                checkSubmissionTypes(it)
                binding.subreddit = it
            }
        }
    }

    private fun setupTabs() {
        submissionFragments.add(SubmissionTextFragment())
        submissionFragments.add(SubmissionLinkFragment())
        submissionFragments.add(SubmissionCrosspostFragment())

        binding.submissionTypes.adapter = PagerAdapter(this)

        TabLayoutMediator(binding.tabs, binding.submissionTypes) { tab, position ->
            tab.text = when (submissionFragments[position]) {
                is SubmissionLinkFragment -> getString(R.string.submittingLinkHint)
                is SubmissionCrosspostFragment -> getString(R.string.submittingCrosspostTitle)
                else -> getString(R.string.submittingTextHint)
            }
        }.attach()
    }

    private fun setupFlairsViewModel(subredditName: String) {
        flairsViewModel = ViewModelProvider(this, SubredditFlairsFactory(
                subredditName,
                FlairType.SUBMISSION,
                App.get().api.subreddit(subredditName),
                App.get().database.flairs()
        )).get(SubredditFlairsViewModel::class.java).apply {
            flairs.observe(this@SubmitActivity) {
                // If we get back an empty list always check again (this will happen if we don't already have
                // flairs in the db already)
                if (it.isEmpty()) {
                    CoroutineScope(IO).launch {
                        refresh()
                    }
                }

                RedditFlairAdapter(this@SubmitActivity, android.R.layout.simple_spinner_item, it as ArrayList<RedditFlair>).apply {
                    binding.flairSpinner.adapter = this
                }
            }

            errors.observe(this@SubmitActivity) {
                // 403 errors occur when the subreddit doesn't allow flairs, so don't show those errors
                // since it's not really an "error" that the user should care about
                if (it.error.code == 403) {
                    binding.flairSpinner.visibility = GONE
                } else {
                    handleGenericResponseErrors(binding.root, it.error, it.throwable)
                }
            }
            // There won't be anything else causing this to loader to load so this is safe
            isLoading.observe(this@SubmitActivity) {
                binding.submissionFlairLoadingIcon.visibility = if (it) {
                    VISIBLE
                } else {
                    INVISIBLE
                }
            }

            // Load flairs right away if data saving isn't enabled. If the list from the ViewModel
            // is empty, the observer will also trigger a load even if data saving is on
            if (!App.get().dataSavingEnabled()) {
                CoroutineScope(IO).launch {
                    refresh()
                }
            }
        }
    }

    private fun checkSubmissionTypes(subreddit: Subreddit) {
        // Check which submissions are allowed on the subreddit, set the ones that arent allowed as disabled
    }

    /**
     * Submits the post based on what is in text tab
     */
    private fun submitText(fragment: SubmissionTextFragment) {
        val title = binding.title.text.toString()
        val nsfw = binding.nsfw.isChecked
        val spoiler = binding.spoiler.isChecked
        val receiveNotifications = binding.sendNotifications.isChecked

        // TODO match the text against a regex looking for links, if it looks like a link ask "It looks like you're only
        //  submitting a link, do you want to submit this as a link post instead of a text post?" (idk if some subs disallow
        //  text posts, but if they do don't ask then)

        val text = fragment.getText()

        CoroutineScope(IO).launch {
            val response = api.subreddit(subreddit.name).submitTextPost(
                    title,
                    text,
                    nsfw,
                    spoiler,
                    receiveNotifications,
                    flairId = getFlairId()
            )

            withContext(Main) {
                onSubmitResponse(response)
            }
        }
    }

    /**
     * Submits the post based on what is in the link tab
     */
    private fun submitLink(fragment: SubmissionLinkFragment) {
        val title = binding.title.text.toString()
        val nsfw = binding.nsfw.isChecked
        val spoiler = binding.spoiler.isChecked
        val receiveNotifications = binding.sendNotifications.isChecked

        val link = fragment.getLink()

        if (link.isBlank()) {
            Snackbar.make(binding.root, R.string.submittingLinkNoLink, Snackbar.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(IO).launch {
            val response = api.subreddit(subreddit.name).submitLinkPost(
                    title,
                    fragment.getLink(),
                    nsfw,
                    spoiler,
                    receiveNotifications,
                    flairId = getFlairId()
            )

            withContext(Main) {
                onSubmitResponse(response)
            }
        }
    }

    /**
     * Submits the post based on what is in the crosspost tab
     */
    private fun submitCrosspost(fragment: SubmissionCrosspostFragment) {
        val title = binding.title.text.toString()
        val nsfw = binding.nsfw.isChecked
        val spoiler = binding.spoiler.isChecked
        val receiveNotifications = binding.sendNotifications.isChecked

        val id = fragment.getCrosspostId()

        if (id.isBlank()) {
            Snackbar.make(binding.root, R.string.submittingCrosspostNoId, Snackbar.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(IO).launch {
            val response = api.subreddit(subreddit.name).submitCrosspost(
                    title,
                    id,
                    nsfw,
                    spoiler,
                    receiveNotifications,
                    flairId = getFlairId()
            )

            withContext(Main) {
                onSubmitResponse(response)
            }
        }
    }

    /**
     * Handles responses when a post has been submitted. If a post was successfully submitted then
     * the activity will be finished and the ID of the newly created post is returned, otherwise a
     * snackbar is shown.
     */
    private fun onSubmitResponse(response: ApiResponse<Submission>) {
        when (response) {
            is ApiResponse.Success -> {
                intent.putExtra(RESULT_POST_ID, response.value.id)
                setResult(RESULT_OK, intent)
                finish()
            }
            is ApiResponse.Error -> {
                handleGenericResponseErrors(binding.root, response.error, response.throwable)
            }
        }
    }

    /**
     * @return The ID of the flair selected, or an empty string if no item is selected
     */
    private fun getFlairId() : String {
        val selectedItem = binding.flairSpinner.selectedItemPosition
        // The first item will is "Select flair", ie. no item selected
        return if (selectedItem == 0) {
            ""
        } else {
            // The actual list of flairs doesn't include the first "Select flair" item
            (binding.flairSpinner.adapter as RedditFlairAdapter).flairs[selectedItem - 1].id
        }
    }


    inner class PagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = submissionFragments.size
        // TODO apparently this shouldn't reuse fragments, but create a new one each time, but it doesn't
        //  seem to cause an issue
        override fun createFragment(position: Int): Fragment = submissionFragments[position]
    }

    class SubmissionTextFragment : Fragment() {
        var binding: SubmissionTextBinding? = null

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            binding = SubmissionTextBinding.inflate(layoutInflater)
            return binding?.root
        }

        override fun onResume() {
            super.onResume()
            binding?.root?.requestLayout()
        }

        override fun onDestroyView() {
            super.onDestroyView()
            binding = null
        }

        fun getText() : String {
            return binding?.markdownInput?.inputText ?: ""
        }
    }

    class SubmissionLinkFragment : Fragment() {
        var binding: SubmissionLinkBinding? = null

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            binding = SubmissionLinkBinding.inflate(layoutInflater)
            return binding?.root
        }

        override fun onResume() {
            super.onResume()
            binding?.root?.requestLayout()
        }

        override fun onDestroyView() {
            super.onDestroyView()
            binding = null
        }

        fun getLink() : String {
            return binding?.linkSubmission?.text.toString()
        }
    }

    class SubmissionCrosspostFragment : Fragment() {
        var binding: SubmissionCrosspostBinding? = null
        val api = App.get().api
        val timer = Timer()
        var timerTask: TimerTask? = null

        /**
         * Map mapping post IDs to a [RedditPost], holding the posts that have been retrieved so far
         */
        val postsMap = HashMap<String, RedditPost>()

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            binding = SubmissionCrosspostBinding.inflate(layoutInflater)
            binding?.crosspostSubmission?.addTextChangedListener(textWatcher)
            return binding?.root
        }

        override fun onResume() {
            super.onResume()
            binding?.root?.requestLayout()
        }

        override fun onDestroyView() {
            super.onDestroyView()
            binding = null
        }

        /**
         * Gets the id input in the input field
         */
        fun getCrosspostId() : String {
            return binding?.crosspostSubmission?.text.toString()
        }

        /**
         * TextWatcher for the crosspost ID input field that retrieves information about the post the
         * ID represents.
         *
         * Posts are stored in [postsMap]
         */
        private val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val id = getCrosspostId()
                // TODO find out what limitations there are on post IDs and match that to not make
                //  api calls that never return an actual post
                if (id.isBlank()) {
                    binding?.crosspostSubmission?.error = null
                    clearPostInfo()
                    return
                }

                val post = postsMap[id]

                // Post previously retrieved, use that instead of making new API call
                if (post != null) {
                    setPostInfo(post)
                } else {
                    // Cancel previous task
                    timerTask?.cancel()
                    timerTask = object : TimerTask() {
                        override fun run() {
                            getPostInfo(id)
                        }
                    }
                    timer.schedule(timerTask, 500L)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not implemented
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Not implemented
            }
        }

        /**
         * Sets the post info displayed to the user
         */
        private fun setPostInfo(redditPost: RedditPost) {
            binding?.crosspostPost?.redditPost = redditPost
            binding?.crosspostPost?.visibility = VISIBLE
        }

        /**
         * Removes the post info from being shown
         */
        private fun clearPostInfo() {
            binding?.crosspostPost?.visibility = GONE
        }

        /**
         * Gets post info from the API
         *
         * @param id The ID of the post to retrieve information for
         */
        private fun getPostInfo(id: String) {
            (context as AppCompatActivity).runOnUiThread {
                binding?.crosspostLoadingIcon?.visibility = VISIBLE
            }

            CoroutineScope(IO).launch {
                val resp = api.post(id).info()

                withContext(Main) {
                    binding?.crosspostLoadingIcon?.visibility = GONE
                }

                when (resp) {
                    is ApiResponse.Success -> {
                        val post = resp.value
                        withContext(Main) {
                            if (post != null) {
                                setPostInfo(post)
                                postsMap[id] = post
                                binding?.crosspostSubmission?.error = null
                            } else {
                                // The ID in the input might have changed since the request was made
                                if (getCrosspostId() == id) {
                                    binding?.crosspostSubmission?.error = getString(R.string.submittingCrosspostPostNotFound)
                                    clearPostInfo()
                                }
                            }
                        }
                    }
                    is ApiResponse.Error -> {

                    }
                }
            }
        }
    }
}