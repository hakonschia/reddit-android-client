package com.example.hakonsreader.activites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.model.Subreddit
import com.example.hakonsreader.api.persistence.AppDatabase
import com.example.hakonsreader.databinding.ActivitySubmitBinding
import com.example.hakonsreader.databinding.SubmissionCrosspostBinding
import com.example.hakonsreader.databinding.SubmissionLinkBinding
import com.example.hakonsreader.databinding.SubmissionTextBinding
import com.example.hakonsreader.misc.InternalLinkMovementMethod
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubmitBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        val subredditName = intent?.extras?.getString(SUBREDDIT_KEY) ?: return

        // Setup with all tabs initially until we know which submissions are supported on the subreddit
        setupTabs()
        binding.tabs.setupWithViewPager(binding.submissionTypes)


        // We need information about the subreddit, try to get it from the local database and if it doesn't exist, get it from
        // the api
        getSubredditInfo(subredditName)

        binding.subredditName = subredditName

        binding.showPreview.setOnClickListener {
            val textFragment = submissionFragments[0] as SubmissionTextFragment
            textFragment.binding?.markdownInput?.showPreviewInPopupDialog()
        }

        // Add a listener to the ViewPager to show/remove the "Show preview" button as it is only for text posts
        binding.submissionTypes.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageSelected(position: Int) {
                binding.showPreview.visibility = if (submissionFragments[position] is SubmissionTextFragment) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageScrollStateChanged(state: Int) {}
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

        binding.submissionTypes.adapter = PagerAdapter(supportFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT)
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
            api.subreddit("hakonschia").submitTextPost(
                    title,
                    text,
                    nsfw,
                    spoiler,
                    receiveNotifications
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
            api.subreddit("hakonschia").submitLinkPost(
                    title,
                    fragment.getLink(),
                    nsfw,
                    spoiler,
                    receiveNotifications
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
            api.subreddit("hakonschia").submitCrosspost(
                    title,
                    id,
                    nsfw,
                    spoiler,
                    receiveNotifications
            )
        }
    }




    inner class PagerAdapter(fragmentManager: FragmentManager, behaviour: Int) : FragmentPagerAdapter(fragmentManager, behaviour) {
        override fun getCount(): Int = submissionFragments.size
        override fun getItem(position: Int): Fragment = submissionFragments[position]

        override fun getPageTitle(position: Int): CharSequence {
            return when (submissionFragments[position]) {
                is SubmissionLinkFragment -> getString(R.string.submittingLinkHint)
                is SubmissionCrosspostFragment -> getString(R.string.submittingCrosspostTitle)
                else -> {
                    getString(R.string.submittingTextHint)
                }
            }
        }
    }

    class SubmissionTextFragment : Fragment() {
        var binding: SubmissionTextBinding? = null

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            binding = SubmissionTextBinding.inflate(layoutInflater)
            return binding?.root
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

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            binding = SubmissionCrosspostBinding.inflate(layoutInflater)
            return binding?.root
        }

        override fun onDestroyView() {
            super.onDestroyView()
            binding = null
        }

        fun getCrosspostId() : String {
            return binding?.crosspostSubmission?.text.toString()
        }
    }
}