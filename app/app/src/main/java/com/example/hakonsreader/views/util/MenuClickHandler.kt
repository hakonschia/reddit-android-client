package com.example.hakonsreader.views.util

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.res.Resources
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hakonsreader.R
import com.example.hakonsreader.api.RedditApi
import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.model.RedditUser
import com.example.hakonsreader.api.persistence.RedditUserInfoDao
import com.example.hakonsreader.databinding.DialogAccountManagementBinding
import com.example.hakonsreader.dialogadapters.OAuthScopeAdapter
import com.example.hakonsreader.fragments.PostsFragment
import com.example.hakonsreader.interfaces.SortableWithTime
import com.example.hakonsreader.misc.*
import com.example.hakonsreader.recyclerviewadapters.AccountsAdapter
import com.example.hakonsreader.states.AppState
import com.example.hakonsreader.states.LoggedInState
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
 * @param user The user represented in the profile popup clicked
 * @param api The API object to use for API calls
 */
fun showPopupForProfile(view: View, user: RedditUser?, api: RedditApi, userInfoDao: RedditUserInfoDao) {
    user ?: return

    if (AppState.getUserInfo()?.userInfo?.id == user.id) {
        showPopupForLoggedInUser(view, api, userInfoDao)
    }
}

private fun showPopupForLoggedInUser(view: View, api: RedditApi, userInfoDao: RedditUserInfoDao) {
    val context = view.context
    val privatelyBrowsing = AppState.loggedInState.value is LoggedInState.PrivatelyBrowsing

    popupMenu {
        style = R.style.Widget_MPM_Menu_Dark_CustomBackground

        section {
            item {
                labelRes = R.string.logOut
                icon = R.drawable.ic_signout
                callback = { AppState.logOut(context) }
            }

            item {
                labelRes = R.string.menuProfileManageAccounts
                icon = R.drawable.ic_baseline_person_24
                callback = { showAccountManagement(view.context, api, userInfoDao) }
            }

            item {
                labelRes = if (privatelyBrowsing) {
                    R.string.menuPrivateBrowsingDisable
                } else {
                    R.string.menuPrivateBrowsingEnable
                }
                icon = R.drawable.ic_incognito
                callback = { AppState.enablePrivateBrowsing(!privatelyBrowsing) }
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

/**
 * Shows a Dialog with the list of accounts stored in the application
 *
 * @param context The context used. This should be an activity, otherwise the application cannot recreate
 * itself when a new account is chosen
 */
fun showAccountManagement(context: Context, api: RedditApi, userInfoDao: RedditUserInfoDao) {
    val app = AppState

    Dialog(context).also {
        val binding = DialogAccountManagementBinding.inflate(LayoutInflater.from(context))
        it.setContentView(binding.root)
        it.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        binding.addAccount.setOnClickListener {
            startLoginIntent(context)
        }

        binding.accounts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = AccountsAdapter().apply {
                CoroutineScope(IO).launch {
                    val accs = userInfoDao.getAllUsers() as MutableList
                    withContext(Main) {
                        binding.hasUsers = accs.isNotEmpty()
                        accounts = accs
                    }
                }

                onItemClicked = { userInfoClicked ->
                    val currentId = when (val state = app.loggedInState.value) {
                        is LoggedInState.LoggedIn -> state.userInfo.userId
                        is LoggedInState.PrivatelyBrowsing -> state.userInfo.userId
                        else -> null
                    }

                    if (currentId != userInfoClicked.userId) {
                        // Not sure what we would do if not
                        if (context is AppCompatActivity) {
                            app.switchAccount(userInfoClicked.userId, context)
                        }
                    }
                }

                onRemoveItemClicked = { userInfoClicked ->
                    val currentId = when (val state = app.loggedInState.value) {
                        is LoggedInState.LoggedIn -> state.userInfo.userId
                        is LoggedInState.PrivatelyBrowsing -> state.userInfo.userId
                        else -> null
                    }

                    // Don't remove the item if it's the currently active one
                    if (currentId != userInfoClicked.userId) {
                        val token = TokenManager.getTokenByUserId(userInfoClicked.userId)
                        if (token != null) {
                            removeItem(userInfoClicked)

                            TokenManager.removeTokenByUserId(userInfoClicked.userId)

                            CoroutineScope(IO).launch {
                                userInfoDao.delete(userInfoClicked)
                                api.accessToken().revoke(token)
                            }
                        }
                    }
                }

                onNsfwClicked = { userInfoClicked, nsfwAccount ->
                    val currentId = when (val state = app.loggedInState.value) {
                        is LoggedInState.LoggedIn -> state.userInfo.userId
                        is LoggedInState.PrivatelyBrowsing -> state.userInfo.userId
                        else -> null
                    }

                    // Another account than the active was clicked, update it in the database
                    if (currentId != null && currentId != userInfoClicked.userId) {
                        userInfoClicked.nsfwAccount = nsfwAccount
                        CoroutineScope(IO).launch {
                            userInfoDao.update(userInfoClicked)
                        }
                    } else {
                        // Update the current account
                        CoroutineScope(IO).launch {
                            app.updateUserInfo(nsfwAccount = nsfwAccount)
                        }
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
    val scopes = ArrayList(listOf(*TokenManager.getToken()!!.scopesAsArray))
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
 * @param postsFragment The fragment to update the sorting for
 * @param view The view clicked. If this view is not a child of a fragment or activity implementing
 * [SortableWithTime] nothing is done
 */
fun showPopupSortWithTime(postsFragment: PostsFragment, view: View) {
    val context = view.context
    val sortText = getSortText(postsFragment.currentSort(), context)
    val timeSortText = postsFragment.currentTimeSort()?.let { getTimeSortText(it, context) }

    val finalTitle = sortText + if (timeSortText != null) " - $timeSortText" else ""

    // The submenu sizes look weird if they are too small, so set to fixed 40 % of screen width
    val submenuSize = (Resources.getSystem().displayMetrics.widthPixels * 0.4f).toInt()

    popupMenu {
        style = R.style.Widget_MPM_Menu_Dark_CustomBackground

        section {
            title = finalTitle

            item {
                labelRes = R.string.sortNew
                callback = { postsFragment.new() }
            }
            item {
                labelRes = R.string.sortHot
                callback = { postsFragment.hot() }
            }

            item {
                labelRes = R.string.sortTop
                hasNestedItems = true
                callback = {
                    // Show menu
                    popupMenu {
                        style = R.style.Widget_MPM_Menu_Dark_CustomBackground
                        fixedContentWidthInPx = submenuSize

                        section {
                            title = context.getString(R.string.sortTop)

                            item {
                                labelRes = R.string.sortNow
                                callback = { postsFragment.top(PostTimeSort.HOUR) }
                            }
                            item {
                                labelRes = R.string.sortToday
                                callback = { postsFragment.top(PostTimeSort.DAY) }
                            }
                            item {
                                labelRes = R.string.sortWeek
                                callback = { postsFragment.top(PostTimeSort.WEEK) }
                            }
                            item {
                                labelRes = R.string.sortMonth
                                callback = { postsFragment.top(PostTimeSort.MONTH) }
                            }
                            item {
                                labelRes = R.string.sortYear
                                callback = { postsFragment.top(PostTimeSort.YEAR) }
                            }
                            item {
                                labelRes = R.string.sortAllTime
                                callback = { postsFragment.top(PostTimeSort.ALL_TIME) }
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
                        fixedContentWidthInPx = submenuSize

                        section {
                            title = context.getString(R.string.sortControversial)

                            item {
                                labelRes = R.string.sortNow
                                callback = { postsFragment.controversial(PostTimeSort.HOUR) }
                            }
                            item {
                                labelRes = R.string.sortToday
                                callback = { postsFragment.controversial(PostTimeSort.DAY) }
                            }
                            item {
                                labelRes = R.string.sortWeek
                                callback = { postsFragment.controversial(PostTimeSort.WEEK) }
                            }
                            item {
                                labelRes = R.string.sortMonth
                                callback = { postsFragment.controversial(PostTimeSort.MONTH) }
                            }
                            item {
                                labelRes = R.string.sortYear
                                callback = { postsFragment.controversial(PostTimeSort.YEAR) }
                            }
                            item {
                                labelRes = R.string.sortAllTime
                                callback = { postsFragment.controversial(PostTimeSort.ALL_TIME) }
                            }
                        }
                    }.show(context, view)
                }
            }
        }

    }.show(context, view)
}