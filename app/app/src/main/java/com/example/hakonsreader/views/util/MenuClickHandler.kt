package com.example.hakonsreader.views.util

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.view.*
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hakonsreader.App
import com.example.hakonsreader.R
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods
import com.example.hakonsreader.dialogadapters.OAuthScopeAdapter
import com.example.hakonsreader.interfaces.SortableWithTime
import com.example.hakonsreader.misc.TokenManager
import com.example.hakonsreader.misc.startLoginIntent
import com.example.hakonsreader.recyclerviewadapters.AccountsAdapter
import com.github.zawadz88.materialpopupmenu.popupMenu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Shows the popup menu for profiles for logged in users
 *
 * @param view The view clicked (where the menu will be attached)
 */
fun showPopupForProfile(view: View) {
    val context = view.context
    val privatelyBrowsing = App.get().isUserLoggedInPrivatelyBrowsing()

    popupMenu {
        style = R.style.Widget_MPM_Menu_Dark_CustomBackground

        section {
            item {
                labelRes = R.string.logOut
                icon = R.drawable.ic_signout
                callback = { App.get().logOut() }
            }

            item {
                labelRes = R.string.menuProfileManageAccounts
                icon = R.drawable.ic_baseline_person_24
                callback = { showAccountManagement(view.context) }
            }

            item {
                labelRes = if (privatelyBrowsing) {
                    R.string.menuPrivateBrowsingDisable
                } else {
                    R.string.menuPrivateBrowsingEnable
                }
                icon = R.drawable.ic_incognito
                callback = { App.get().enablePrivateBrowsing(!privatelyBrowsing) }
            }

            // TODO find a better icon for this. The idea for a list icon is "this is a list of
            //  what the app can do". Not sure what a "privileges" icon would even be
            item {
                labelRes = R.string.menuShowApplicationAccessExplanations
                icon = R.drawable.ic_format_list_bulleted_24dp
                callback = { showApplicationPrivileges(context, view.parent) }
            }
        }
    }.show(context, view)
}

private fun showAccountManagement(context: Context) {
    val app = App.get()

    Dialog(context).also {
        it.setContentView(R.layout.dialog_account_management)
        it.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        it.findViewById<Button>(R.id.addAccount).setOnClickListener {
            startLoginIntent(context)
        }

        it.findViewById<RecyclerView>(R.id.accounts).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AccountsAdapter().apply {
                CoroutineScope(IO).launch {
                    val accs = app.userInfoDatabase.userInfo().getAllUsers() as MutableList
                    withContext(Main) {
                        accounts = accs
                    }
                }

                onItemClicked = { userInfoClicked ->
                    val currentId = app.currentUserInfo?.accessToken?.userId
                    if (currentId != null && currentId != userInfoClicked.accessToken.userId) {
                        app.switchAccount(userInfoClicked.accessToken)
                    }
                }

                onRemoveItemClicked = { userInfoClicked ->
                    val currentId = app.currentUserInfo?.accessToken?.userId
                    // Don't remove the item if it's the currently active one
                    if (currentId != null && currentId != userInfoClicked.accessToken.userId) {
                        removeItem(userInfoClicked)
                        CoroutineScope(IO).launch {
                            app.userInfoDatabase.userInfo().delete(userInfoClicked)
                        }
                    }
                }
                onNsfwClicked = { userInfoClicked, nsfwAccount ->
                    val currentId = app.currentUserInfo?.accessToken?.userId

                    // Another account than the active was clicked, update it in the database
                    if (currentId != null && currentId != userInfoClicked.accessToken.userId) {
                        userInfoClicked.nsfwAccount = nsfwAccount
                        CoroutineScope(IO).launch {
                            app.userInfoDatabase.userInfo().update(userInfoClicked)
                        }
                    } else {
                        // Update the current account
                        app.updateUserInfo(nsfwAccount = nsfwAccount)
                    }
                }
            }
        }

        it.show()
    }
}

/**
 * Shows a popup of the applications OAuth privileges
 */
private fun showApplicationPrivileges(context: Context, parent: ViewParent) {
    // TODO if scopes have been added to the application that isn't in the stored token, show which are missing as well
    val scopes = ArrayList(Arrays.asList(*TokenManager.getToken()!!.scopesAsArray))
    val adapter = OAuthScopeAdapter(context, R.layout.list_item_oauth_explanation, scopes)
    val title = LayoutInflater.from(context).inflate(R.layout.dialog_title_oauth_explanation, parent as ViewGroup, false)
    AlertDialog.Builder(context)
            .setCustomTitle(title)
            .setAdapter(adapter, null)
            .show()
}

/**
 * Shows a popup menu to allow a list to change how it should be sorted. The menu shown here
 * includes time sorts for sorts such as top and controversial
 *
 * @param view The view clicked. If this view is not a child of a fragment or activity implementing
 * [SortableWithTime] nothing is done
 */
fun showPopupSortWithTime(view: View) {
    val f = FragmentManager.findFragment<Fragment>(view)
    val context = view.context
    val sortable: SortableWithTime = if (f is SortableWithTime) {
        f
    } else if (context is SortableWithTime) {
        context
    } else {
        return
    }

    val sortText = when (sortable.currentSort()) {
        SortingMethods.NEW -> context.getString(R.string.sortNew)
        SortingMethods.HOT -> context.getString(R.string.sortHot)
        SortingMethods.TOP -> context.getString(R.string.sortTop)
        SortingMethods.CONTROVERSIAL -> context.getString(R.string.sortControversial)
    }

    val timeSortText = when (sortable.currentTimeSort()) {
        PostTimeSort.HOUR -> context.getString(R.string.sortNow)
        PostTimeSort.DAY -> context.getString(R.string.sortToday)
        PostTimeSort.WEEK -> context.getString(R.string.sortWeek)
        PostTimeSort.MONTH -> context.getString(R.string.sortMonth)
        PostTimeSort.YEAR -> context.getString(R.string.sortYear)
        PostTimeSort.ALL_TIME -> context.getString(R.string.sortAllTime)
        else -> null
    }

    val finalTitle = sortText + if (timeSortText != null) " - $timeSortText" else ""

    // The submenu sizes look weird if they are too small, so set to fixed 40 % of screen width
    val submenuSize = App.get().screenWidth * 0.4f

    popupMenu {
        style = R.style.Widget_MPM_Menu_Dark_CustomBackground

        section {
            title = finalTitle

            item {
                labelRes = R.string.sortNew
                callback = { sortable.new() }
            }
            item {
                labelRes = R.string.sortHot
                callback = { sortable.hot() }
            }

            item {
                labelRes = R.string.sortTop
                hasNestedItems = true
                callback = {
                    // Show menu
                    popupMenu {
                        style = R.style.Widget_MPM_Menu_Dark_CustomBackground
                        fixedContentWidthInPx = submenuSize.toInt()

                        section {
                            title = context.getString(R.string.sortTop)

                            item {
                                labelRes = R.string.sortNow
                                callback = { sortable.top(PostTimeSort.HOUR) }
                            }
                            item {
                                labelRes = R.string.sortToday
                                callback = { sortable.top(PostTimeSort.DAY) }
                            }
                            item {
                                labelRes = R.string.sortWeek
                                callback = { sortable.top(PostTimeSort.WEEK) }
                            }
                            item {
                                labelRes = R.string.sortMonth
                                callback = { sortable.top(PostTimeSort.MONTH) }
                            }
                            item {
                                labelRes = R.string.sortYear
                                callback = { sortable.top(PostTimeSort.YEAR) }
                            }
                            item {
                                labelRes = R.string.sortAllTime
                                callback = { sortable.top(PostTimeSort.ALL_TIME) }
                            }
                        }
                    }.show(context, view)
                }
            }

            item {
                labelRes = R.string.sortControversial
                hasNestedItems = true
                callback = {
                    popupMenu {
                        style = R.style.Widget_MPM_Menu_Dark_CustomBackground
                        fixedContentWidthInPx = submenuSize.toInt()

                        section {
                            title = context.getString(R.string.sortControversial)

                            item {
                                labelRes = R.string.sortNow
                                callback = { sortable.controversial(PostTimeSort.HOUR) }
                            }
                            item {
                                labelRes = R.string.sortToday
                                callback = { sortable.controversial(PostTimeSort.DAY) }
                            }
                            item {
                                labelRes = R.string.sortWeek
                                callback = { sortable.controversial(PostTimeSort.WEEK) }
                            }
                            item {
                                labelRes = R.string.sortMonth
                                callback = { sortable.controversial(PostTimeSort.MONTH) }
                            }
                            item {
                                labelRes = R.string.sortYear
                                callback = { sortable.controversial(PostTimeSort.YEAR) }
                            }
                            item {
                                labelRes = R.string.sortAllTime
                                callback = { sortable.controversial(PostTimeSort.ALL_TIME) }
                            }
                        }
                    }.show(context, view)
                }
            }
        }

    }.show(context, view)
}
