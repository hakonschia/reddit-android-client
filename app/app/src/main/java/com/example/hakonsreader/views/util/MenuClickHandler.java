package com.example.hakonsreader.views.util;

import android.content.Context;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.enums.PostTimeSort;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.interfaces.SortableWithTime;
import com.example.hakonsreader.misc.Util;
import com.google.android.material.snackbar.Snackbar;

import static com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_SHORT;

/**
 * Class containing click handlers for menus
 */
public class MenuClickHandler {
    private static final String TAG = "MenuClickHandler";

    private MenuClickHandler() { }


    /**
     * Shows the popup for comments. Based on the user status, a different popup is shown
     * (ie. if the logged in user is the comment poster a different popup is shown)
     *
     * @param view The view clicked
     * @param comment The comment the popup is for
     */
    public static void showPopupForCommentExtra(View view, RedditComment comment) {
        if (App.getStoredUser().getName().equalsIgnoreCase(comment.getAuthor())) {
            showPopupForCommentExtraForLoggedInUser(view, comment);
        }
    }

    /**
     * Shows the popup for comments for when the comment is posted by the user currently logged in
     *
     * @param view The view clicked
     * @param comment The comment the popup is for
     */
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
     * @param view The view clicked. If this view is not a child of a fragment or activity implementing
     *             {@link SortableWithTime} nothing is done
     */
    public static void showPopupSortWithTime(View view) {
        Fragment f = FragmentManager.findFragment(view);
        Context context = view.getContext();

        SortableWithTime sortable;

        if (f instanceof SortableWithTime) {
            sortable = (SortableWithTime) f;
        } else if (context instanceof SortableWithTime) {
            sortable = (SortableWithTime) context;
        } else {
            return;
        }
        // TODO the menu should show what is the currently selected sort (probably have to adjust the interface for a getter method)

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
                    } else {
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
                    } else {
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
