package com.example.hakonsreader.activites;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.PopupMenu;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.hakonsreader.R;
import com.example.hakonsreader.databinding.ActivityWebViewBinding;


/**
 * Activity that displays a WebView with a given URL. Use {@link WebViewActivity#URL_KEY} to send
 * the URL to load
 */
public class WebViewActivity extends AppCompatActivity {
    private static final String TAG = "WebViewActivity";

    /**
     * The key used to send the URL to open in the WebView
     */
    public static final String URL_KEY = "url";

    private ActivityWebViewBinding binding;


    // If we want to show/hide the toolbar on scrolling, we need to nest the WebView in a scrolling
    // container (NestedScrollView), but that disables zooming
    // NestedWebView from: https://github.com/takahirom/webview-in-coordinatorlayout allows for toolbar
    // show/hide, but it messes up zooming in a very weird way

    @SuppressLint("setJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWebViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String url = getIntent().getExtras().getString(URL_KEY);

        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                binding.webViewUrl.setText(uri.toString());
                return super.shouldOverrideUrlLoading(view, request);
            }
        });
        binding.webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);

                // Animate the progress change
                int oldProgress = binding.progressBar.getProgress();
                binding.progressBar.setProgress(newProgress);

                // If the old progress is 100 don't animate the change, as it would go from completely full
                // to whatever the new progress is, which makes it go backwards which looks weird
                if (oldProgress != 100) {
                    ObjectAnimator animation = ObjectAnimator.ofInt(binding.progressBar, "progress", oldProgress, newProgress);
                    animation.setDuration(150);
                    animation.setInterpolator(new LinearInterpolator());
                    animation.start();
                }

                // Finished loading, fade the progress bar out so that it's visible that it finishes
                if (newProgress == 100) {
                    binding.progressBar.animate().setDuration(500).alpha(0).start();
                } else {
                    binding.progressBar.setAlpha(1f);
                    binding.progressBar.setVisibility(View.VISIBLE);
                }
            }
        });

        WebSettings webViewSettings = binding.webView.getSettings();
        webViewSettings.setJavaScriptEnabled(true);
        // Some websites wont load without this enabled (such as imgur albums)
        webViewSettings.setDomStorageEnabled(true);
        webViewSettings.setDisplayZoomControls(false);
        webViewSettings.setBuiltInZoomControls(true);

        binding.webView.loadUrl(url);
        binding.webViewUrl.setText(url);

        // Close the web view (ie. finish the activity) with the button in the toolbar
        binding.webViewClose.setOnClickListener(v -> finish());
        binding.webViewMenu.setOnClickListener(this::openMenu);

        binding.refreshWebView.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(this, R.color.colorAccent));
        binding.refreshWebView.setOnRefreshListener(() -> {
            binding.webView.reload();
            // The progress bar will show the progress so we don't need the refresh icon
            binding.refreshWebView.setRefreshing(false);
        });
    }

    @Override
    public void onBackPressed() {
        if (binding.webView.canGoBack()) {
            // shouldOverrideUrlLoading isn't called when going back, so update the URL here
            WebBackForwardList mWebBackForwardList = binding.webView.copyBackForwardList();
            if (mWebBackForwardList.getCurrentIndex() > 0) {
                String url = mWebBackForwardList.getItemAtIndex(mWebBackForwardList.getCurrentIndex() - 1).getUrl();
                binding.webViewUrl.setText(url);
            }

            binding.webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Opens the more menu in the toolbar
     *
     * @param view The view to attach the menu to
     */
    private void openMenu(View view) {
        PopupMenu menu = new PopupMenu(this, view);
        menu.inflate(R.menu.web_view_more);

        menu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.webViewMenuRefresh) {
                binding.webView.reload();
                return true;
            } else if (itemId == R.id.webViewMenuOpenInBrowser) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(binding.webView.getUrl()));
                startActivity(i);
                return true;
            }

            return false;
        });

        menu.show();
    }
}
