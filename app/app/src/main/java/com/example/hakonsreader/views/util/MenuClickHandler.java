package com.example.hakonsreader.views.util;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.PopupMenu;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.enums.PostTimeSort;
import com.example.hakonsreader.dialogadapters.OAuthScopeAdapter;
import com.example.hakonsreader.interfaces.SortableWithTime;
import com.example.hakonsreader.misc.TokenManager;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Class containing click handlers for menus
 */
public class MenuClickHandler {
    private static final String TAG = "MenuClickHandler";

    private MenuClickHandler() { }

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

        boolean isPrivatelyBrowsing = App.Companion.get().getApi().isPrivatelyBrowsing();
        if (isPrivatelyBrowsing) {
            MenuItem savedItem = menu.getMenu().findItem(R.id.menuBrowsePrivately);
            savedItem.setTitle(view.getContext().getString(R.string.menuPrivateBrowsingDisable));
        }

        menu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menuLogOut) {
                App.Companion.get().logOut();
                return true;
            } else if (itemId == R.id.menuBrowsePrivately) {
                App.Companion.get().enablePrivateBrowsing(!isPrivatelyBrowsing);
                return true;
            } else if (itemId == R.id.menuApplicationPrivileges) {
                showApplicationPrivileges(view.getContext(), view.getParent());
                return true;
            }

            return false;
        });

        menu.show();
    }

    /**
     * Shows a popup of the applications OAuth privileges
     */
    private static void showApplicationPrivileges(Context context, ViewParent parent) {
        // TODO if scopes have been added to the application that isn't in the stored token, show which are missing as well
        ArrayList<String> scopes = new ArrayList<>(Arrays.asList(TokenManager.getToken().getScopesAsArray()));
        OAuthScopeAdapter adapter = new OAuthScopeAdapter(context, R.layout.list_item_oauth_explanation, scopes);

        View title = LayoutInflater.from(context).inflate(R.layout.dialog_title_oauth_explanation, (ViewGroup) parent, false);

        new AlertDialog.Builder(context)
                .setCustomTitle(title)
                .setAdapter(adapter, null)
                .show();
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
                menu.setOnMenuItemClickListener(subItem -> {
                    int subItemId = subItem.getItemId();
                    sortable.top(getTimeSortFromId(subItemId));
                    return true;
                });
                return true;
            } else if (itemId == R.id.sortControversial) {
                MenuItem subMenu = menu.getMenu().findItem(R.id.sortControversial);
                menu.getMenuInflater().inflate(R.menu.sort_times, subMenu.getSubMenu());
                menu.setOnMenuItemClickListener(subItem -> {
                    int subItemId = subItem.getItemId();
                    sortable.controversial(getTimeSortFromId(subItemId));
                    return true;
                });
                return true;
            }

            return false;
        });

        menu.show();
    }


    /**
     * Retrieves the correct {@link PostTimeSort} based on an ID res
     *
     * @param id The ID to retrieve a sort for (the IDs from {@code sort_times.xml}
     * @return The corresponding {@link PostTimeSort}. Default if no match is {@link PostTimeSort#ALL_TIME}
     */
    private static PostTimeSort getTimeSortFromId(@IdRes int id) {
        PostTimeSort timeSort;

        if (id == R.id.sortNow) {
            timeSort = PostTimeSort.HOUR;
        } else if (id == R.id.sortToday) {
            timeSort = PostTimeSort.DAY;
        } else if (id == R.id.sortWeek) {
            timeSort = PostTimeSort.WEEK;
        } else if(id == R.id.sortMonth) {
            timeSort = PostTimeSort.MONTH;
        } else if (id == R.id.sortYear) {
            timeSort = PostTimeSort.YEAR;
        } else {
            timeSort = PostTimeSort.ALL_TIME;
        }

        return timeSort;
    }
}
