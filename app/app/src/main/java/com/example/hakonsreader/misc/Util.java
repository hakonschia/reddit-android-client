package com.example.hakonsreader.misc;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.activities.LogInActivity;
import com.example.hakonsreader.activities.MainActivity;
import com.example.hakonsreader.api.exceptions.ArchivedException;
import com.example.hakonsreader.api.exceptions.InvalidAccessTokenException;
import com.example.hakonsreader.api.exceptions.RateLimitException;
import com.example.hakonsreader.api.exceptions.ThreadLockedException;
import com.example.hakonsreader.api.responses.GenericError;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT;
import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG;

public class Util {
    private static final String TAG = "Util";

    private Util() { }


    /**
     * Handles generic errors that are common for all API responses and shows a snackbar to the user
     *
     * @param parent The view to attach the snackbar to
     * @param error The error for the request
     * @param t Throwable from the request
     */
    public static void handleGenericResponseErrors(View parent, GenericError error, Throwable t) {
        int code = error.getCode();
        String reason = error.getReason();
        t.printStackTrace();

        if (t instanceof IOException) {
            Util.showNoInternetSnackbar(parent);
        } else if (t instanceof InvalidAccessTokenException) {
            if (App.Companion.get().isUserLoggedInPrivatelyBrowsing()) {
                Util.showPrivatelyBrowsingSnackbar(parent);
            } else {
                Util.showNotLoggedInSnackbar(parent);
            }
        } else if (t instanceof ThreadLockedException) {
            Util.showThreadLockedException(parent);
        } else if (t instanceof ArchivedException) {
            Util.showArchivedException(parent);
        } else if (GenericError.REQUIRES_REDDIT_PREMIUM.equals(reason)) {
            Util.showRequiresRedditPremiumSnackbar(parent);
        } else if (code == 400) {
            // 400 requests are "Bad request" which means something went wrong (Reddit are generally pretty
            // "secretive" with their error responses, they only give a code)
            Util.showBadRequestSnackbar(parent);
        } else if (code == 403) {
            Util.showForbiddenErrorSnackbar(parent);
        } else if (code == 429 || t instanceof RateLimitException) {
            // 429 = Too many requests. Reddit sometimes returns a 429, or 200 with a "RATELIMIT" error message
            Util.showTooManyRequestsSnackbar(parent);
        } else if (code >= 500) {
            Util.showGenericServerErrorSnackbar(parent);
        } else {
            Util.showUnknownError(parent);
        }
    }

    /**
     * Creates and shows a snackbar for errors caused by no internet connection
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showNoInternetSnackbar(View parent) {
        Snackbar.make(parent, parent.getResources().getString(R.string.noInternetConnection), LENGTH_SHORT).show();
    }

    /**
     * Creates and shows a snackbar for when an action was attempted that requires a logged in user,
     * but private browsing is currently enabled.
     *
     * <p>The snackbar includes a button to disable private browsing</p>
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showPrivatelyBrowsingSnackbar(View parent) {
        Snackbar snackbar = Snackbar.make(parent, parent.getResources().getString(R.string.privatelyBrowsingError), LENGTH_LONG);
        Context context = parent.getContext();

        snackbar.setAction(context.getString(R.string.disable), v -> {
            App.Companion.get().enablePrivateBrowsing(false);
        });
        snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.colorAccent));
        snackbar.show();
    }

    /**
     * Creates and shows a snackbar for when an action was attempted that requires the user to be logged in
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showNotLoggedInSnackbar(View parent) {
        Snackbar snackbar = Snackbar.make(parent, parent.getResources().getString(R.string.notLoggedInError), LENGTH_LONG);
        Context context = parent.getContext();

        snackbar.setAction(context.getString(R.string.log_in), v -> {
            // If getContext instance of MainActivity we can set the navbar item to profile and, otherwise create activity for logging in
            if (context instanceof MainActivity) {
                ((MainActivity)context).selectProfileNavBar();
            } else {
                // Otherwise we can open an activity showing a login fragment
                context.startActivity(new Intent(context, LogInActivity.class));
            }
        });
        snackbar.setActionTextColor(ContextCompat.getColor(context, R.color.colorAccent));
        snackbar.show();
    }

    /**
     * Creates and shows a snackbar for errors caused by a thread being locked
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showThreadLockedException(View parent) {
        Snackbar.make(parent, parent.getResources().getString(R.string.threadLockedError), LENGTH_SHORT).show();
    }

    /**
     * Creates and shows a snackbar for errors caused by a listing being archived
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showArchivedException(View parent) {
        Snackbar.make(parent, parent.getResources().getString(R.string.listingArchivedError), LENGTH_SHORT).show();
    }

    /**
     * Creates and shows a snackbar for errors caused by a 400 bad request error
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showBadRequestSnackbar(View parent) {
        Snackbar.make(parent, parent.getResources().getString(R.string.badRequestError), LENGTH_SHORT).show();
    }

    /**
     * Creates and shows a snackbar for errors caused by no internet connection
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showForbiddenErrorSnackbar(View parent) {
        // 403 errors are generally when the access token is outdated and new functionality has been
        // added that requires more OAuth scopes
        Snackbar.make(parent, parent.getResources().getString(R.string.forbiddenError), LENGTH_SHORT).show();
    }

    /**
     * Creates and shows a snackbar for generic server errors
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showGenericServerErrorSnackbar(View parent) {
        Snackbar.make(parent, parent.getResources().getString(R.string.genericServerError), LENGTH_SHORT).show();
    }

    /**
     * Creates and shows a snackbar for errors caused by too many requests sent
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showTooManyRequestsSnackbar(View parent) {
        Snackbar.make(parent, parent.getResources().getString(R.string.tooManyRequestsError), LENGTH_SHORT).show();
    }

    /**
     * Creates and shows a snackbar for errors caused by too many requests sent
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showErrorLoggingInSnackbar(View parent) {
        Snackbar.make(parent, parent.getResources().getString(R.string.errorLoggingIn), LENGTH_SHORT).show();
    }

    /**
     * Creates and shows a snackbar for errors caused by no internet connection
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showUnknownError(View parent) {
        Snackbar.make(parent, parent.getResources().getString(R.string.unknownError), LENGTH_SHORT).show();
    }

    /**
     * Creates and shows a snackbar for errors caused by an action being attempted that requires
     * Reddit premium
     *
     * @param parent The view to attach the snackbar to
     */
    public static void showRequiresRedditPremiumSnackbar(View parent) {
        Snackbar.make(parent, parent.getResources().getString(R.string.requiresRedditPremium), LENGTH_SHORT).show();
    }


    /**
     * Creates the text for text age text fields. For a shorter text see 
     * {@link Util#createAgeTextShortened(Resources, Duration)}
     *
     * <p>Formats to make sure that it says 3 hours, 5 minutes etc. based on what makes sense</p>
     *
     * @param resources Resources to retrieve strings from
     * @param time The time to format as
     * @return The time formatted as a string
     */
    public static String createAgeText(Resources resources, Duration time) {
        String format;
        long t;

        if ((t = time.toDays()) > 0) {
            format = resources.getQuantityString(R.plurals.postAgeDays, (int) t);
        } else if ((t = time.toHours()) > 0) {
            format = resources.getQuantityString(R.plurals.postAgeHours, (int) t);
        } else {
            t = time.toMinutes();
            if (t < 1) {
                return resources.getString(R.string.postAgeJustPosted);
            }
            format = resources.getQuantityString(R.plurals.postAgeMinutes, (int) t);
        }

        return String.format(Locale.getDefault(), format, t);
    }


    /**
     * Creates the text for text age text fields with a shorter text than with
     * {@link Util#createAgeText(Resources, Duration)}
     *
     * <p>Formats to make sure that it says 3h, 5m etc. based on what makes sense</p>
     *
     * @param resources Resources to retrieve strings from
     * @param time The time to format as
     * @return The time formatted as a string
     */
    public static String createAgeTextShortened(Resources resources, Duration time) {
        String format;
        long t;

        if ((t = time.toDays()) > 0) {
            format = resources.getString(R.string.postAgeDaysShortened, (int) t);
        } else if ((t = time.toHours()) > 0) {
            format = resources.getString(R.string.postAgeHoursShortened, (int) t);
        } else {
            t = time.toMinutes();
            if (t < 1) {
                return resources.getString(R.string.postAgeJustPostedShortened);
            }
            format = resources.getString(R.string.postAgeMinutesShortened, (int) t);
        }

        return String.format(Locale.getDefault(), format, t);
    }

    /**
     * Creates the text for text age on trending subreddits
     *
     * <p>Formats to make sure that it says 3 hours, 5 minutes etc. based on what makes sense</p>
     *
     * @param tv The text view to set the text on
     * @param time The time to format as
     */
    public static void setAgeTextTrendingSubreddits(TextView tv, Duration time) {
        final Resources resources = tv.getResources();
        String format;
        long t;

        if ((t = time.toDays()) > 0) {
            format = resources.getQuantityString(R.plurals.postAgeDays, (int) t);
        } else if ((t = time.toHours()) > 0) {
            format = resources.getQuantityString(R.plurals.postAgeHours, (int) t);
        } else {
            t = time.toMinutes();
            if (t < 1) {
                tv.setText(R.string.trendingSubredditsLastUpdatedNow);
                return;
            }
            format = resources.getQuantityString(R.plurals.postAgeMinutes, (int) t);
        }

        String str = String.format(Locale.getDefault(), format, t);
        tv.setText(resources.getString(R.string.trendingSubredditsLastUpdated, str));
    }

    /**
     * Create a duration string in the format of "mm:ss" that can be used in videos
     *
     * @param seconds The amount of seconds to display
     * @return A string formatted as "mm:ss"
     */
    public static String createVideoDuration(int seconds) {
        return String.format("%02d:%02d", (seconds % 3600) / 60, seconds % 60);
    }

    /**
     * Converts dp to pixels
     * @param dp The amount of dp to convert
     * @param res The resources
     * @return The pixel amount of *dp*
     */
    public static int dpToPixels(float dp, Resources res) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                res.getDisplayMetrics()
        );
    }
}
