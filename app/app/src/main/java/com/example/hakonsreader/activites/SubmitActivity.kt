package com.example.hakonsreader.activites

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.model.RedditPost
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.model.flairs.SubmissionFlair
import com.example.hakonsreader.api.persistence.AppDatabase
import com.example.hakonsreader.api.responses.ApiResponse
import com.example.hakonsreader.databinding.ActivitySubmitBinding
import com.example.hakonsreader.databinding.SubmissionCrosspostBinding
import com.example.hakonsreader.databinding.SubmissionLinkBinding
import com.example.hakonsreader.databinding.SubmissionTextBinding
import com.example.hakonsreader.misc.InternalLinkMovementMethod
import com.example.hakonsreader.dialogadapters.SubmissionFlairAdapter
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
 * Activity for submitting a post to a subreddit
 */
class SubmitActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SubmitActivity"

        /**
         * The key used to tell the name of the subreddit being submitted to
         */
        const val SUBREDDIT_KEY = "submittingToSubredditName"
    }

    private val api = App.get().api
    private val database = AppDatabase.getInstance(this)

    private lateinit var binding:  ActivitySubmitBinding
    private lateinit var subreddit: Subreddit
    private val submissionFragments = ArrayList<Fragment>()
    private var submissionFlairs = ArrayList<SubmissionFlair>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubmitBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        val subredditName = intent?.extras?.getString(SUBREDDIT_KEY) ?: return

        // Setup with all tabs initially until we know which submissions are supported on the subreddit
        setupTabs()

        // We need information about the subreddit, try to get it from the local database and if it doesn't exist, get it from
        // the api
        getSubredditInfo(subredditName)
        getSubmissionFlairs(subredditName)

        binding.subredditName = subredditName

        binding.showPreview.setOnClickListener {
            val textFragment = submissionFragments[0] as SubmissionTextFragment
            textFragment.binding?.markdownInput?.showPreviewInPopupDialog()
        }

        binding.submissionTypes.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.showPreview.visibility = if (submissionFragments[position] is SubmissionTextFragment) {
                    VISIBLE
                } else {
                    // Use INVISIBLE instead of GONE since the "Submit post" button relies on the position of the preview
                    INVISIBLE
                }
            }
        })

        binding.submitPost.setOnClickListener {
            if (binding.title.text?.length == 0) {
                // Too short
                Snackbar.make(binding.root, R.string.submittingNoTitle, Snackbar.LENGTH_LONG).show()

                return@setOnClickListener
            } else if (binding.title.text?.length!! > resources.getInteger(R.integer.submissionTitleMaxLength)) {
                // Too long
                Snackbar.make(binding.root, R.string.submittingTitleTooLong, Snackbar.LENGTH_LONG).show()

                return@setOnClickListener
            }

            // Check which page is active, submit based on that
            when (val fragment = submissionFragments[binding.submissionTypes.currentItem]) {
                is SubmissionTextFragment -> submitText(fragment)
                is SubmissionLinkFragment -> submitLink(fragment)
                is SubmissionCrosspostFragment -> submitCrosspost(fragment)
            }
        }
    }

    private fun getSubredditInfo(subredditName: String) {
        CoroutineScope(IO).launch {
            val sub: Subreddit? = database.subreddits().get(subredditName)

            if (sub != null) {
                subreddit = sub
                withContext(Main) {
                    updateViews()
                    checkSubmissionTypes(subreddit)
                }
            }  else {
                // TODO Get from API
            }
        }
    }

    /**
     * Calls the API to get the submission flairs for this subreddit
     */
    private fun getSubmissionFlairs(subredditName: String) {
        binding.submissionFlairLoadingIcon.visibility = VISIBLE
        CoroutineScope(IO).launch {
            val response = api.subreddit(subredditName).submissionFlairs()
            withContext(Main) {
                when (response) {
                    is ApiResponse.Success -> {
                        onSubmissionFlairResponse(response.value)
                    }
                    // Not sure what makes sense to do on these errors, if flairs are required then
                    // it matters, if not then it's not critical if it fails
                    is ApiResponse.Error -> {
                        // If the sub doesn't allow flairs, a 403 is returned
                        binding.submissionFlairLoadingIcon.visibility = GONE
                        binding.flairSpinner.visibility = GONE
                    }
                }
            }
        }
    }

    /**
     * Handles successful responses for submission flairs. The loading icon is removed and
     * [ActivitySubmitBinding.flairSpinner] is updated with the flairs.
     *
     * If no flairs are returned, then the spinner view is removed
     *
     * @param flairs The flairs retrieved
     */
    private fun onSubmissionFlairResponse(flairs: List<SubmissionFlair>) {
        submissionFlairs = flairs as ArrayList<SubmissionFlair>

        if (submissionFlairs.isEmpty()) {
            binding.flairSpinner.visibility = GONE
            return
        }

        val adapter = SubmissionFlairAdapter(this@SubmitActivity, android.R.layout.simple_spinner_item, submissionFlairs)
        binding.flairSpinner.adapter = adapter
        binding.submissionFlairLoadingIcon.visibility = GONE
    }

    /**
     * Updates the views in the activity based on [subreddit]
     */
    private fun updateViews() {
        binding.subredditSubmitText.movementMethod = InternalLinkMovementMethod.getInstance(this)

        val submitTextAdjusted = App.get().adjuster.adjust(subreddit.submitText)
        App.get().mark.setMarkdown(binding.subredditSubmitText, submitTextAdjusted)
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
                else -> {
                    getString(R.string.submittingTextHint)
                }
            }
        }.attach()
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
            api.subreddit(subreddit.name).submitTextPost(
                    title,
                    text,
                    nsfw,
                    spoiler,
                    receiveNotifications,
                    flairId =  getFlairId()
            )
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
            api.subreddit(subreddit.name).submitLinkPost(
                    title,
                    fragment.getLink(),
                    nsfw,
                    spoiler,
                    receiveNotifications,
                    flairId =  getFlairId()
            )
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
            api.subreddit(subreddit.name).submitCrosspost(
                    title,
                    id,
                    nsfw,
                    spoiler,
                    receiveNotifications,
                    flairId =  getFlairId()
            )
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
            submissionFlairs[selectedItem - 1].id
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
        val api: RedditApi = App.get().api
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
                binding?.crosspostLoadingIcon?.onCountChange(true)
            }

            CoroutineScope(IO).launch {
                val resp = api.post(id).info()

                withContext(Main) {
                    binding?.crosspostLoadingIcon?.onCountChange(false)
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