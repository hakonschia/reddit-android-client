package com.example.hakonsreader.activites

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.animation.LinearInterpolator
import android.webkit.*
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hakonsreader.R
import com.example.hakonsreader.activites.WebViewActivity.Companion.URL
import com.example.hakonsreader.databinding.ActivityWebViewBinding

/**
 * Activity that displays a WebView with a given URL
 *
 * Use [URL] to send the URL to load
 */
class WebViewActivity : AppCompatActivity() {
    companion object {
        const val URL = "url"
    }


    private var binding: ActivityWebViewBinding? = null


    // If we want to show/hide the toolbar on scrolling, we need to nest the WebView in a scrolling
    // container (NestedScrollView), but that disables zooming
    // NestedWebView from: https://github.com/takahirom/webview-in-coordinatorlayout allows for toolbar
    // show/hide, but it messes up zooming in a very weird way

    @SuppressLint("setJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)

        setContentView(binding?.root)
        val url = intent.extras?.getString(URL)
        if (url == null) {
            finish()
            return
        }

        binding?.webView?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                binding?.webViewUrl?.text = uri.toString()
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        binding?.webView?.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)

                binding?.let {
                    // Animate the progress change
                    val oldProgress = it.progressBar.progress
                    it.progressBar.progress = newProgress

                    // If the old progress is 100 don't animate the change, as it would go from completely full
                    // to whatever the new progress is, which makes it go backwards which looks weird
                    if (oldProgress != 100) {
                        val animation = ObjectAnimator.ofInt(it.progressBar, "progress", oldProgress, newProgress)
                        animation.duration = 150
                        animation.interpolator = LinearInterpolator()
                        animation.start()
                    }

                    // Finished loading, fade the progress bar out so that it's visible that it finishes
                    if (newProgress == 100) {
                        it.progressBar.animate()?.setDuration(500)?.alpha(0f)?.start()
                    } else {
                        it.progressBar.alpha = 1f
                        it.progressBar.visibility = View.VISIBLE
                    }
                }

            }
        }
        val webViewSettings = binding?.webView?.settings!!
        webViewSettings.javaScriptEnabled = true
        // Some websites wont load without this enabled (such as imgur albums)
        webViewSettings.domStorageEnabled = true
        webViewSettings.displayZoomControls = false
        webViewSettings.builtInZoomControls = true

        binding?.webView?.loadUrl(url)
        binding?.webViewUrl?.text = url

        // Close the web view (ie. finish the activity) with the button in the toolbar
        binding?.webViewClose?.setOnClickListener { v -> finish() }
        binding?.webViewMenu?.setOnClickListener { view: View -> openMenu(view) }

        binding?.refreshWebView?.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(this, R.color.colorAccent))
        binding?.refreshWebView?.setOnRefreshListener {
            binding?.webView?.reload()
            // The progress bar will show the progress so we don't need the refresh icon
            binding?.refreshWebView?.isRefreshing = false
        }
    }

    override fun onBackPressed() {
        if (binding?.webView?.canGoBack() == true) {
            // shouldOverrideUrlLoading isn't called when going back, so update the URL here
            val mWebBackForwardList: WebBackForwardList = binding?.webView?.copyBackForwardList()!!
            if (mWebBackForwardList.currentIndex > 0) {
                val url = mWebBackForwardList.getItemAtIndex(mWebBackForwardList.currentIndex - 1).url
                binding?.webViewUrl?.text = url
            }
            binding?.webView?.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    /**
     * Opens the more menu in the toolbar
     *
     * @param view The view to attach the menu to
     */
    private fun openMenu(view: View) {
        val menu = PopupMenu(this, view)
        menu.inflate(R.menu.web_view_more)
        menu.setOnMenuItemClickListener { item: MenuItem ->
            val itemId = item.itemId
            if (itemId == R.id.webViewMenuRefresh) {
                binding?.webView?.reload()

                return@setOnMenuItemClickListener true
            } else if (itemId == R.id.webViewMenuOpenInBrowser) {
                val i = Intent(Intent.ACTION_VIEW, Uri.parse(binding?.webView?.url))
                startActivity(i)

                return@setOnMenuItemClickListener true
            }
            false
        }
        menu.show()
    }
}