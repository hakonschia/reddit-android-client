package com.example.hakonsreader.activites;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebBackForwardList;
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

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                binding.refreshWebView.setRefreshing(false);
            }
        });

        WebSettings webViewSettings = binding.webView.getSettings();
        webViewSettings.setJavaScriptEnabled(true);
        // Some websites wont load without this enabled (such as imgur albums)
        webViewSettings.setDomStorageEnabled(true);
        binding.webView.loadUrl(url);
        binding.webViewUrl.setText(url);

        // Close the web view (ie. finish the activity) with the button in the toolbar
        binding.webViewClose.setOnClickListener(v -> finish());
        binding.webViewMenu.setOnClickListener(this::openMenu);

        binding.refreshWebView.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(this, R.color.colorAccent));
        binding.refreshWebView.setOnRefreshListener(binding.webView::reload);
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
                // Use the SwipeRefreshLayout refresher for this as well
                binding.refreshWebView.setRefreshing(true);
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
