package com.example.hakonsreader.activites;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.utils.LinkUtils;

import java.util.List;

import retrofit2.http.Url;


/**
 * Activity to dispatch links. This activity never renders any UI, but serves as a proxy that redirects
 * to the correct activity
 */
public class DispatcherActivity extends AppCompatActivity {
    private static final String TAG = "DispatcherActivity";

    /**
     * The key used to transfer the URL to dispatch
     *
     * <p>Example URL: https://www.reddit.com/r/</p>
     */
    public static final String URL_KEY = "url";


    private boolean fadeTransition = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent startIntent = getIntent();
        Uri uri = startIntent.getData();
        String url;

        // Activity started from a URL intent
        if (uri != null) {
            url = uri.getPath();
        } else {
            // Activity started from a manual intent
            Bundle data = startIntent.getExtras();

            if (data == null) {
                finish();
                return;
            }

            url = data.getString(URL_KEY);
        }

        if (url == null) {
            finish();
            return;
        }

        Log.d(TAG, "onCreate: Dispatcher dispacthing " + url);

        // If the URL can be converted to a direct link (eg. as an image) ensure it is
        url = LinkUtils.convertToDirectUrl(url);
        Intent intent = createIntent(url);

        if (fadeTransition) {
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }

        startActivity(intent);
    }


    public Intent createIntent(String url) {

        // The intent to start the new activity
        Intent intent;

        Uri asUri = Uri.parse(url);
        List<String> pathSegments = asUri.getPathSegments();

        String lastSegment = "";
        // Get the last segment to check for file extensions
        if (pathSegments.size() > 0) {
            lastSegment = pathSegments.get(pathSegments.size() - 1);
        }

        if (url.matches(LinkUtils.SUBREDDIT_REGEX)) {
            // First is "r", second is the subreddit
            String subreddit = pathSegments.get(1);

            intent = new Intent(this, SubredditActivity.class);
            intent.putExtra(SubredditActivity.SUBREDDIT_KEY, subreddit);

        } else if (url.matches(LinkUtils.USER_REGEX)) {
            // intent = new Intent(this, ProfileFragment.class);
            // TODO create activity for profiles
            intent = new Intent(this, SubredditActivity.class);
            intent.putExtra(SubredditActivity.SUBREDDIT_KEY, "globaloffensive");

        } else if (url.matches(LinkUtils.POST_REGEX)) {
            // The URL will look like: reddit.com/r/<subreddit>/comments/<postId/...
            String postId = pathSegments.get(3);

            intent = new Intent(this, PostActivity.class);
            intent.putExtra(PostActivity.POST_ID_KEY, postId);

        } else if (lastSegment.matches(".+(.png|.jpeg|.jpg)$")) {
            intent = new Intent(this, ImageActivity.class);
            intent.putExtra(ImageActivity.IMAGE_URL, url);

            fadeTransition = true;

            // TODO check if URL is an imgur image and add .png


        } else if (url.matches(LinkUtils.GIF_REGEX)) {
            intent = new Intent(this, VideoActivity.class);
            // TODO for this to work I need to rewrite ContentVideo so there is an additional class
            //  that is only responsible for the video player which only needs the URL
            //  and then send that

        } else {
            // If we don't know how to handle the URL pass it to a web view
            intent = new Intent(this, WebViewActivity.class);
            intent.putExtra(WebViewActivity.URL_KEY, url);
        }

        return intent;
    }


    // onResume is called when activity is returned to by exiting another, and when it starts initially
    // onPause is only called when the activity pauses, such as when starting another activity, so if
    // we have paused previously when in onResume we can finish the activity
    boolean paused = false;

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            finish();
        }
    }
}
