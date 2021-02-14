package com.example.hakonsreader.activites

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.webkit.*
import androidx.core.content.ContextCompat
import com.example.hakonsreader.R
import com.example.hakonsreader.activites.WebViewActivity.Companion.URL
import com.example.hakonsreader.databinding.ActivityWebViewBinding
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar

/**
 * Activity that displays a WebView with a given URL
 *
 * Use [URL] to send the URL to load
 */
class WebViewActivity : BaseActivity() {
    companion object {
        const val URL = "url"
    }

    private lateinit var binding: ActivityWebViewBinding

    // If we want to show/hide the toolbar on scrolling, we need to nest the WebView in a scrolling
    // container (NestedScrollView), but that disables zooming
    // NestedWebView from: https://github.com/takahirom/webview-in-coordinatorlayout allows for toolbar
    // show/hide, but it messes up zooming in a very weird way

    @SuppressLint("setJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val url = intent.extras?.getString(URL)
        if (url == null) {
            finish()
            return
        }

        with (binding) {
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    webViewUrl.text = uri.toString()
                    return super.shouldOverrideUrlLoading(view, request)
                }
            }
            webView.webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)

                    // Animate the progress change
                    val oldProgress = progressBar.progress
                    progressBar.progress = newProgress

                    // If the old progress is 100 don't animate the change, as it would go from completely full
                    // to whatever the new progress is, which makes it go backwards which looks weird
                    if (oldProgress != 100) {
                        val animation = ObjectAnimator.ofInt(progressBar, "progress", oldProgress, newProgress)
                        animation.duration = 150
                        animation.interpolator = LinearInterpolator()
                        animation.start()
                    }

                    // Finished loading, fade the progress bar out so that it's visible that it finishes
                    if (newProgress == 100) {
                        progressBar.animate().setDuration(500).alpha(0f).start()
                    } else {
                        progressBar.alpha = 1f
                        progressBar.visibility = View.VISIBLE
                    }
                }
            }

            webView.settings.run {
                javaScriptEnabled = true
                // Some websites wont load without this enabled (such as imgur albums)
                domStorageEnabled = true
                displayZoomControls = false
                builtInZoomControls = true
            }

            webView.loadUrl(url)
            webViewUrl.text = url

            // Close the web view (ie. finish the activity) with the button in the toolbar
            webViewClose.setOnClickListener { finish() }
            webViewMenu.setOnClickListener { view -> openMenu(view) }

            refreshWebView.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(this@WebViewActivity, R.color.colorAccent))
            refreshWebView.setOnRefreshListener {
                webView.reload()
                // The progress bar will show the progress so we don't need the refresh icon
                refreshWebView.isRefreshing = false
            }
        }
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            // shouldOverrideUrlLoading isn't called when going back, so update the URL here
            val mWebBackForwardList: WebBackForwardList = binding.webView.copyBackForwardList()
            if (mWebBackForwardList.currentIndex > 0) {
                val url = mWebBackForwardList.getItemAtIndex(mWebBackForwardList.currentIndex - 1).url
                binding.webViewUrl.text = url
            }
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Opens the more menu in the toolbar
     *
     * @param view The view to attach the menu to
     */
    private fun openMenu(view: View) {
        popupMenu {
            style = R.style.Widget_MPM_Menu_Dark_CustomBackground

            section {
                item {
                    labelRes = R.string.webViewCopyLink
                    icon = R.drawable.ic_content_copy_24

                    callback = {
                        val clipboard = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Link", binding.webView.url)
                        clipboard.setPrimaryClip(clip)
                        Snackbar.make(view, R.string.linkCopied, BaseTransientBottomBar.LENGTH_SHORT).show()
                    }
                }

                item {
                    labelRes = R.string.webViewRefresh
                    icon = R.drawable.ic_refresh_24dp

                    callback = { binding.webView.reload() }
                }

                item {
                    labelRes = R.string.webViewOpenInBrowser
                    icon = R.drawable.ic_launch_24dp
                    callback = {
                        Intent(Intent.ACTION_VIEW, Uri.parse(binding.webView.url)).run {
                            startActivity(this)
                        }
                    }
                }
            }
        }.show(this, view)
    }
}