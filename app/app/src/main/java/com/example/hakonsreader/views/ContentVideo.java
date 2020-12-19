package com.example.hakonsreader.views;

import android.content.Context;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.model.Image;
import com.example.hakonsreader.api.model.RedditVideo;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.databinding.ContentVideoBinding;
import com.example.hakonsreader.enums.ShowNsfwPreview;
import com.example.hakonsreader.interfaces.OnVideoManuallyPaused;

import java.util.List;

/**
 * View for playing video posts from Reddit. Use {@link #isRedditPostVideoPlayable(RedditPost)} to
 * check if the post can be displayed with this view
 */
public class ContentVideo extends Content {
    private static final String TAG = "PostContentVideo";


    /**
     * The key used for extra information about the timestamp of the video
     *
     * <p>The value stored with this key will be a {@code long}</p>
     */
    public static final String EXTRA_TIMESTAMP = "videoTimestamp";

    /**
     * The key used for extra information about the playback state of a video
     *
     * <p>The value stored with this key will be a {@code boolean}</p>
     */
    public static final String EXTRA_IS_PLAYING = "isPlaying";

    /**
     * The key used for extra information about the playback state of a video
     *
     * <p>The value stored with this key will be a {@code boolean}</p>
     */
    public static final String EXTRA_SHOW_CONTROLS = "showControls";

    /**
     * The key used for extra information about the volume of the video
     *
     * <p>The value stored with this key will be a {@code boolean}</p>
     */
    public static final String EXTRA_VOLUME = "volume";


    private final VideoPlayer player;


    public ContentVideo(Context context) {
        this(context, null, 0);
    }
    public ContentVideo(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public ContentVideo(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ContentVideoBinding binding = ContentVideoBinding.inflate(LayoutInflater.from(context), this, true);
        player = binding.player;
    }

    /**
     * Sets the callback for when a video post has been manually paused
     *
     * @param onVideoManuallyPaused The callback
     */
    public void setOnVideoManuallyPaused(@Nullable OnVideoManuallyPaused onVideoManuallyPaused) {
        //player.setOnManuallyPaused(() -> onVideoManuallyPaused.postPaused(this));
    }

    /**
     * Creates the exo player and updates the view
     */
    @Override
    protected void updateView() {
        setThumbnailUrl();
        setVideo();

        if (redditPost.isNsfw() && App.Companion.get().dontCacheNSFW()) {
            player.setCacheVideo(false);
        }

        if (App.Companion.get().muteVideoByDefault()) {
            player.toggleVolume(false);
        }

        // Kind of a really bad way to make the video resize :)  When the post is opened
        // the video player won't automatically resize, so if the height of the view has been updated
        // manually (> 0, ie. not wrap_content or match_parent), set those params on the player as well
        // It would probably be better if this could be done in VideoPlayer instead automatically
        getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            ViewGroup.LayoutParams layoutParams = getLayoutParams();
            if (layoutParams.height > 0) {
                player.setLayoutParams(layoutParams);
            }
        });
    }

    @Override
    public List<Pair<View, String>> getTransitionViews() {
        List<Pair<View, String>> views = super.getTransitionViews();
        views.add(new Pair<>(player, player.getTransitionName()));
        return views;
    }

    /**
     * Called when the video has been selected. If the user has enabled auto play the video will start playing
     *
     * If the users setting allows for autoplay then it is autoplayed, if the video is marked as a
     * spoiler it will never play, if marked as NSFW it will only play if the user has allowed NSFW autoplay
     */
    @Override
    public void viewSelected() {
        // TODO if the video has already been played, then we can resume (for all) (should this be a setting? maybe)
        if (redditPost.isSpoiler()) {
            return;
        }

        if (redditPost.isNsfw()) {
            if (App.Companion.get().autoPlayNsfwVideos()) {
                player.play();
            }
        } else if (App.Companion.get().autoPlayVideos()) {
            player.play();
        }
    }

    /**
     * Pauses the video playback
     */
    @Override
    public void viewUnselected() {
        player.pause();
    }

    /**
     * Retrieve a bundle of information that can be useful for saving the state of the post
     *
     * @return A bundle that might include state variables
     */
    @Override
    @NonNull
    public Bundle getExtras() {
        Bundle extras = new Bundle();

        extras.putLong(EXTRA_TIMESTAMP, getCurrentPosition());
        extras.putBoolean(EXTRA_IS_PLAYING, isPlaying());
        extras.putBoolean(EXTRA_SHOW_CONTROLS, isControllerShown());
        extras.putBoolean(EXTRA_VOLUME, player.isAudioOn());

        return extras;
    }

    /**
     * Sets the extras for the video.
     *
     * @param extras The bundle of data to use. This should be the same bundle as retrieved with
     *               {@link ContentVideo#getExtras()}
     */
    @Override
    public void setExtras(@NonNull Bundle extras) {
        long timestamp = extras.getLong(EXTRA_TIMESTAMP);
        boolean isPlaying = extras.getBoolean(EXTRA_IS_PLAYING);
        boolean showController = extras.getBoolean(EXTRA_SHOW_CONTROLS);
        boolean volumeOn = extras.getBoolean(EXTRA_VOLUME);

        // Video has been played previously so make sure the player is prepared
        if (timestamp != 0) {
            player.prepare();

            // If the video was paused, remove the thumbnail so it shows the correct frame
            if (!isPlaying) {
                player.removeThumbnail();
            }
        }

        player.setPosition(timestamp);

        if (isPlaying) {
            player.play();
        } else {
            // Probably unnecessary?
            player.pause();
        }
        setControllerVisible(showController);
        player.toggleVolume(volumeOn);
    }

    /**
     * Sets the URL on {@link #player} and updates the size/duration if possible
     */
    private void setVideo() {
        // Get either the video, or the GIF
        RedditVideo video = redditPost.getVideo();
        if (video == null) {
            video = redditPost.getVideoGif();
        }

        String url = null;

        if (video != null) {
            url = video.getDashUrl();

            // If we have a "RedditVideo" we can set the duration now
            player.setVideoDuration(video.getDuration());
            player.setDashVideo(true);
            player.setVideoWidth(video.getWidth());
            player.setVideoHeight(video.getHeight());
        } else {
            Image gif = redditPost.getMp4Source();
            if (gif != null) {
                url = gif.getUrl();
                player.setVideoWidth(gif.getWidth());
                player.setVideoHeight(gif.getHeight());
            }
        }

        if (url != null) {
            player.setUrl(url);
        } else {
            // Show some sort of error
        }
    }

    private void setThumbnailUrl() {
        // Loading the blurred/no image etc. is very copied from ContentImage and should be
        // generified so it's not duplicated, but cba to fix that right now

        // post.getThumbnail() returns an image which is very low quality, the source preview
        // has the same dimensions as the video itself
        Image image = redditPost.getSourcePreview();
        String imageUrl = image != null ? image.getUrl() : null;

        // Don't show thumbnail for NSFW posts
        String obfuscatedUrl = null;
        int noImageId = -1;
        if (redditPost.isNsfw()) {
            ShowNsfwPreview show = App.Companion.get().showNsfwPreview();

            switch (show) {
                case NORMAL:
                    // Do nothing, load imageUrl as is
                    break;

                case BLURRED:
                    obfuscatedUrl = getObfuscatedUrl();
                    // If we don't have a URL to show then show the NSFW drawable instead as a fallback
                    if (obfuscatedUrl == null) {
                        noImageId = R.drawable.ic_baseline_image_nsfw_200;
                    }
                    break;

                case NO_IMAGE:
                    noImageId = R.drawable.ic_baseline_image_nsfw_200;
                    break;
            }
        } else if (redditPost.isSpoiler()) {
            obfuscatedUrl = getObfuscatedUrl();
            // If we don't have a URL to show then show the NSFW drawable instead as a fallback
            if (obfuscatedUrl == null) {
                noImageId = R.drawable.ic_baseline_image_nsfw_200;
            }
        }

        if (noImageId != -1) {
            player.setThumbnailDrawable(noImageId);
        } else {
            player.setThumbnailUrl(obfuscatedUrl != null ? obfuscatedUrl : imageUrl);
        }
    }

    /**
     * Retrieves the position in the video
     *
     * @return The amount of milliseconds into the video
     */
    public long getCurrentPosition() {
        return player.getPosition();
    }

    /**
     * Retrieve the current playback state
     *
     * @return True if there is a video playing
     */
    public boolean isPlaying() {
        return player.isPlaying();
    }

    /**
     * Retrieve the state of the controllers of the video
     *
     * @return True if the controller is visible
     */
    public boolean isControllerShown() {
        return player.isControllerVisible();
    }

    /**
     * Releases the video to free up its resources
     */
    public void release() {
        player.release();
    }

    /**
     * Set the visibility of the controller of the video
     *
     * @param visible If true the controller will be shown
     */
    public void setControllerVisible(boolean visible) {
        if (visible) {
            player.showController();
        } else {
            player.hideController();
        }
    }

    /**
     * Ensures that the video fits the screen
     */
    public void fitScreen() {
        // TODO this is a pretty bad way of doing it as the controls get pushed to the bottom of the screen even
        //  if the video itself isn't
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        setLayoutParams(params);
    }

    /**
     * Retrieves the obfuscated image URL to use
     *
     * @return An URL pointing to an image, or {@code null} of no obfuscated images were found
     */
    private String getObfuscatedUrl() {
        List<Image> obfuscatedPreviews = redditPost.getObfuscatedPreviewImages();

        if (obfuscatedPreviews != null && !obfuscatedPreviews.isEmpty()) {
            // Obfuscated previews that are high res are still fairly showing sometimes, so
            // get the lowest quality one as that will not be very easy to tell what it is
            return obfuscatedPreviews.get(0).getUrl();
        }

        return null;
    }


    /**
     * Checks if a {@link RedditPost} is possible to play as a video. Even if {@link RedditPost#getPostType()}
     * indicates that the post is a video, it might not include any supported video formats since
     * old posts might have different content.
     *
     * @param post The post to check
     * @return True if the post can be played as a video, false otherwise
     */
    public static boolean isRedditPostVideoPlayable(@NonNull RedditPost post) {
        // TODO YouTube videos can be loaded with the YouTube Android Player API (https://developers.google.com/youtube/android/player)
        return post.getVideo() != null || post.getVideoGif() != null || post.getMp4Source() != null;
    }
}
