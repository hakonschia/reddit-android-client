package com.example.hakonsreader.activites;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hakonsreader.R;
import com.example.hakonsreader.api.utils.LinkUtils;
import com.jakewharton.processphoenix.ProcessPhoenix;

import java.util.List;


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

    /**
     * Matches variants of "reddit.com"
     *
     * <p>Matches:
     * <ol>
     *     <li>http</li>
     *     <li>https</li>
     *     <li>.com</li>
     *     <li>.com/</li>
     * </ol>
     * </p>
     */
    public static final String REDDIT_HOME_PAGE_URL = "^http(s)?://(www.)?((reddit\\.com)|(redd.it))(/)?$";


    private boolean fadeTransition = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent startIntent = getIntent();
        Uri uri = startIntent.getData();
        String url;

        // Activity started from a URL intent
        if (uri != null) {
            url = uri.toString();
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

        Log.d(TAG, "Dispatching " + url);

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
        if (!pathSegments.isEmpty()) {
            lastSegment = pathSegments.get(pathSegments.size() - 1);
        }

        if (url.matches(REDDIT_HOME_PAGE_URL)) {
            // When the app is started from a "reddit.com" intent we could load the front page as
            // its own subreddit activity, but it makes more sense that this actually loads the application
            // Since this activity is started without MainActivity being created we can just recreate the
            // application from scratch, which makes it so the application starts as clicking on the app icon
            // Alternatively this could probably just resolve to MainActivity directly
            ProcessPhoenix.triggerRebirth(this);
            return null;
        } else if (url.matches(LinkUtils.SUBREDDIT_REGEX_COMBINED)) {
            // First is "r", second is the subreddit
            String subreddit = pathSegments.get(1);

            intent = new Intent(this, SubredditActivity.class);
            intent.putExtra(SubredditActivity.SUBREDDIT_KEY, subreddit);

        } else if (url.matches(LinkUtils.USER_REGEX)) {
            // Same as with subreddits, first is "u", second is the username
            String username = pathSegments.get(1);

            intent = new Intent(this, ProfileActivity.class);
            intent.putExtra(ProfileActivity.USERNAME_KEY, username);

        } else if (url.matches(LinkUtils.POST_REGEX)) {
            // The URL will look like: reddit.com/r/<subreddit>/comments/<postId/...
            String postId = pathSegments.get(3);

            intent = new Intent(this, PostActivity.class);
            intent.putExtra(PostActivity.POST_ID_KEY, postId);

            // Link to a comment chain
            // The URL will look like: reddit.com/r/<subreddit>/comments/<postId/<postTitle>/<commentId>
            if (pathSegments.size() >= 6) {
                intent.putExtra(PostActivity.COMMENT_ID_CHAIN, pathSegments.get(5));
            }

            // TODO when the post is in a "user" subreddit it doesnt work
            //  eg: https://www.reddit.com/user/HyperBirchyBoy/comments/jbkw1f/moon_landing_with_benny_hill_and_sped_up/?utm_source=share&utm_medium=ios_app&utm_name=iossmf
        } else if (url.matches(LinkUtils.POST_REGEX_NO_SUBREDDIT)) {
            // The URL will look like: reddit.com/comments/<postId>
            String postId = pathSegments.get(1);

            intent = new Intent(this, PostActivity.class);
            intent.putExtra(PostActivity.POST_ID_KEY, postId);

        } else if (url.matches(LinkUtils.POST_SHORTENED_URL_REGEX)) {
            // The URL will look like: redd.it/<postId>
            String postId = pathSegments.get(0);

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
            // Redirect to the corresponding application for the url, or WebViewActivity if no app is found

            // Create an intent that would redirect to another app if installed
            intent = new Intent(Intent.ACTION_VIEW, asUri);

            // Find all activities this intent would resolve to
            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> intentActivities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

            // To check if the intent matches an app we need to find the default browser as that
            // will usually be in the list of intent activities
            Intent defaultBrowserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://"));
            ResolveInfo defaultBrowserInfo = packageManager.resolveActivity(defaultBrowserIntent, PackageManager.MATCH_DEFAULT_ONLY);

            boolean appActivityFound = false;

            // Check if there are intents not leading to a browser
            for (ResolveInfo intentActivity : intentActivities) {
                if (!intentActivity.activityInfo.packageName.equals(defaultBrowserInfo.activityInfo.packageName)) {
                    appActivityFound = true;
                    break;
                }
            }

            // If no activity found, open in WebView (internal browser)
            if (!appActivityFound) {
                intent = new Intent(this, WebViewActivity.class);
                intent.putExtra(WebViewActivity.URL_KEY, url);
            } else {
                Log.d(TAG, "createIntent: no specific intent found");
            }
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
