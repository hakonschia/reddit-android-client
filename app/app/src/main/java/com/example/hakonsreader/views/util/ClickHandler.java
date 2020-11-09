package com.example.hakonsreader.views.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.activites.ImageActivity;
import com.example.hakonsreader.activites.ProfileActivity;
import com.example.hakonsreader.activites.SubredditActivity;
import com.example.hakonsreader.api.enums.PostTimeSort;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.interfaces.SortableWithTime;
import com.example.hakonsreader.misc.Util;
import com.google.android.material.snackbar.Snackbar;

import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT;

/**
 * Various click handlers that can be used as click listeners for data binding
 */
public class ClickHandler {
    private static final String TAG = "ClickHandler";

    private ClickHandler() { }


    public static void emptyClick() { };

    /**
     * Empty function to consume long click events. This can be used in XML to handle
     * a conditional {@code onLongClick} where one operand should be empty
     *
     * @return Always {@code true}
     */
    public static boolean emptyClickBoolean() { return true; }

    /**
     * Opens an activity with the selected subreddit
     *
     * @param view The view itself is ignored, but this cannot be null as the context is needed
     * @param subreddit The subreddit to open
     */
    public static void openSubredditInActivity(View view, String subreddit) {
        Context context = view.getContext();
        Activity activity = (Activity)context;

        // Don't open another activity if we are already in that subreddit (because why would you)
        // TODO also check if we are in PostActivity and the post was started from the same subreddit
        // TODO also check for fragments
        if (activity instanceof SubredditActivity) {
            SubredditActivity subredditActivity = (SubredditActivity)activity;
            if (subredditActivity.getSubredditName().equals(subreddit)) {
                return;
            }
        }

        // Send some data like what sub it is etc etc so it knows what to load
        Intent intent = new Intent(context, SubredditActivity.class);
        intent.putExtra(SubredditActivity.SUBREDDIT_KEY, subreddit);

        activity.startActivity(intent);

        // Slide the activity in
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     * Opens an activity to show a users profile
     *
     * @param view The view itself is ignored, but this cannot be null as the context is needed
     * @param username The username of the profile to open
     */
    public static void openProfileInActivity(View view, String username) {
        Context context = view.getContext();
        Activity activity = (Activity)context;

        // Send some data like what sub it is etc etc so it knows what to load
        Intent intent = new Intent(context, ProfileActivity.class);
        intent.putExtra(ProfileActivity.USERNAME_KEY, username);

        activity.startActivity(intent);

        // Slide the activity in
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }


    /**
     * Opens an image in fullscreen
     *
     * @param view The view itself is ignored, but this cannot be null as the context is needed
     * @param imageUrl The URL to the image
     */
    public static void openImageInFullscreen(View view, String imageUrl) {
        Context context = view.getContext();
        Activity activity = (Activity)context;

        // Send some data like what sub it is etc etc so it knows what to load
        Intent intent = new Intent(context, ImageActivity.class);
        intent.putExtra(ImageActivity.IMAGE_URL, imageUrl);

        activity.startActivity(intent);

        // Slide the activity in
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
    /**
     * Opens an image in fullscreen
     *
     * @param context The context to start ImageActivity
     * @param imageUrl The URL to the image
     */
    public static void openImageInFullscreen(Context context, String imageUrl) {
        Activity activity = (Activity)context;

        // Send some data like what sub it is etc etc so it knows what to load
        Intent intent = new Intent(context, ImageActivity.class);
        intent.putExtra(ImageActivity.IMAGE_URL, imageUrl);

        activity.startActivity(intent);

        // Slide the activity in
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }


    /**
     * Function that can be used as onLongClick that displays a toast with the views
     * {@code contentDescription}
     *
     * @param view The view to display the description for
     * @return True (event consumed)
     */
    public static boolean showToastWithContentDescription(View view) {
        CharSequence description = view.getContentDescription();

        if (description != null) {
            String desc = view.getContentDescription().toString();

            if (!desc.isEmpty()) {
                Toast.makeText(view.getContext(), description, Toast.LENGTH_SHORT).show();
            }
        }

        return true;
    }

    public static void showPopupForCommentExtra(View view, RedditComment comment) {
        if (App.getStoredUser().getName().equalsIgnoreCase(comment.getAuthor())) {
            showPopupForCommentExtraForLoggedInUser(view, comment);
        }
    }

    public static void showPopupForCommentExtraForLoggedInUser(View view, RedditComment comment) {
        PopupMenu menu = new PopupMenu(view.getContext(), view);
        menu.inflate(R.menu.comment_extra_by_user);

        menu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menuDeleteComment) {
                Log.d(TAG, "showPopupForCommentExtraForLoggerInUser: Deleting comment");
                // This won't actually return anything valid, so we just assume the comment was deleted
                // This should update the adapter probably?
                App.get().getApi().comment(comment.getId()).delete(response -> Snackbar.make(view, R.string.commentDeleted, LENGTH_SHORT).show(), ((error, t) -> {
                    Util.handleGenericResponseErrors(view, error, t);
                }));
                return true;
            }

            return false;
        });
        
        menu.show();
    }


    /**
     * Click handler for the profile "more" (kebab) button. Shows a popup menu customized
     * based on if it should be shown for a logged in user or not
     *
     * @param view The view clicked (where the menu will be attached)
     * @param loggedInUser True if the button is clicked for a logged in user
     */
    public static void showPopupForProfile(View view, boolean loggedInUser) {
        Log.d(TAG, "showPopupForProfile: " + loggedInUser);
        if (loggedInUser) {
            showPopupForProfileForLoggedInUser(view);
        } else {
            showPopupForProfileForNonLoggedInUser(view);
        }
    }

    /**
     * Shows the popup menu for profiles for logged in users
     *
     * @param view The view clicked (where the menu will be attached)
     */
    public static void showPopupForProfileForLoggedInUser(View view) {
        PopupMenu menu = new PopupMenu(view.getContext(), view);
        menu.inflate(R.menu.profile_menu_logged_in_user);

        menu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menuLogOut) {
                // log out etc etc
                App.get().logOut();
                return true;
            }

            return false;
        });

        menu.show();
    }

    /**
     * Shows the popup menu for profiles for non-logged in users
     *
     * @param view The view clicked (where the menu will be attached)
     */
    public static void showPopupForProfileForNonLoggedInUser(View view) {

    }


    /**
     * Shows a popup menu to allow a list to change how it should be sorted. The menu shown here
     * includes time sorts for sorts such as top and controversial
     *
     * @param view The view clicked. If this is not in a fragment implementing {@link SortableWithTime}
     *             nothing is done
     */
    public static void showPopupSortWithTime(View view) {
        Fragment f = FragmentManager.findFragment(view);

        if (!(f instanceof SortableWithTime)) {
            return;
        }

        SortableWithTime sortable = (SortableWithTime) f;
        Context context = view.getContext();

        PopupMenu menu = new PopupMenu(context, view);
        menu.inflate(R.menu.sort_menu_with_time);

        menu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.sortNew) {
                sortable.newSort();
                return true;
            } else if (itemId == R.id.sortHot) {
                sortable.hot();
                return true;
            } else if (itemId == R.id.sortTop) {
                // If top/controversial inflate new menu with sort_menu_of_time
                MenuItem subMenu = menu.getMenu().findItem(R.id.sortTop);
                menu.getMenuInflater().inflate(R.menu.sort_times, subMenu.getSubMenu());

                // TODO fix this listener so that we dont have to have almost the exact same code
                //  with the only difference being calling top() or controversial()
                menu.setOnMenuItemClickListener(subItem -> {
                    int subItemId = subItem.getItemId();

                    PostTimeSort timeSort;

                    if (subItemId == R.id.sortToday) {
                        // TODO find out what is correct of today/hour/now
                        timeSort = PostTimeSort.HOUR;
                    } else if (subItemId == R.id.sortWeek) {
                        timeSort = PostTimeSort.WEEK;
                    } else if(subItemId == R.id.sortMonth) {
                        timeSort = PostTimeSort.MONTH;
                    } else if (subItemId == R.id.sortYear) {
                        timeSort = PostTimeSort.YEAR;
                    }   else {
                        timeSort = PostTimeSort.ALL_TIME;
                    }

                    sortable.top(timeSort);
                    return true;
                });
                return true;
            } else if (itemId == R.id.sortControversial) {
                MenuItem subMenu = menu.getMenu().findItem(R.id.sortControversial);
                menu.getMenuInflater().inflate(R.menu.sort_times, subMenu.getSubMenu());
                menu.setOnMenuItemClickListener(subItem -> {
                    int subItemId = subItem.getItemId();

                    PostTimeSort timeSort;

                    if (subItemId == R.id.sortToday) {
                        // TODO find out what is correct of today/hour/now
                        timeSort = PostTimeSort.HOUR;
                    } else if (subItemId == R.id.sortWeek) {
                        timeSort = PostTimeSort.WEEK;
                    } else if(subItemId == R.id.sortMonth) {
                        timeSort = PostTimeSort.MONTH;
                    } else if (subItemId == R.id.sortYear) {
                        timeSort = PostTimeSort.YEAR;
                    }   else {
                        timeSort = PostTimeSort.ALL_TIME;
                    }

                    sortable.controversial(timeSort);
                    return true;
                });
                return true;
            }

            return false;
        });

        menu.show();
    }
}
