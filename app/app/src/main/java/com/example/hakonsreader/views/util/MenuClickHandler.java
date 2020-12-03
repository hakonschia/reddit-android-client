package com.example.hakonsreader.views.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.hakonsreader.App;
import com.example.hakonsreader.R;
import com.example.hakonsreader.api.RedditApi;
import com.example.hakonsreader.api.enums.PostTimeSort;
import com.example.hakonsreader.api.interfaces.OnResponse;
import com.example.hakonsreader.api.model.RedditComment;
import com.example.hakonsreader.api.model.RedditPost;
import com.example.hakonsreader.api.model.RedditUser;
import com.example.hakonsreader.fragments.ProfileFragment;
import com.example.hakonsreader.interfaces.SortableWithTime;
import com.example.hakonsreader.misc.Util;
import com.example.hakonsreader.recyclerviewadapters.CommentsAdapter;
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
     * @param adapter The RecyclerView adapter the comment is in
     */
    public static void showPopupForCommentExtra(View view, RedditComment comment, CommentsAdapter adapter) {
        // If the menu is clicked before the comment loads, it will be passed as null
        if (comment == null) {
            return;
        }
        RedditUser user = App.getStoredUser();

        if (!App.get().isUserLoggedInPrivatelyBrowsing() && user != null && user.getUsername().equalsIgnoreCase(comment.getAuthor())) {
            showPopupForCommentExtraForLoggedInUser(view, comment, adapter);
        } else {
            showPopupForCommentExtraForNonLoggedInUser(view, comment, adapter);
        }
    }

    /**
     * Shows the popup for comments for when the comment is posted by the user currently logged in
     *
     * <p>See also: {@link MenuClickHandler#showPopupForCommentExtra(View, RedditComment, CommentsAdapter)}</p>
     *
     * @param view The view clicked (where the popup will be attached)
     * @param comment The comment the popup is for
     * @param adapter The RecyclerView adapter the comment is in
     */
    public static void showPopupForCommentExtraForLoggedInUser(View view, RedditComment comment, CommentsAdapter adapter) {
        PopupMenu menu = new PopupMenu(view.getContext(), view);
        menu.inflate(R.menu.comment_extra_by_user);
        menu.inflate(R.menu.comment_extra_generic_for_all_users);

        // Add mod specific if user is a mod in the subreddit the post is in
        // TODO only top-level comments can be stickied, but any comment can be distinguished
        if (comment.isUserMod()) {
            if (comment.getDepth() == 0) {
                menu.inflate(R.menu.comment_extra_by_user_user_is_mod);
            } else {
                menu.inflate(R.menu.comment_extra_by_user_user_is_mod_no_sticky);
            }

            // Set text to "Undistinguish"
            if (comment.isMod()) {
                MenuItem modItem = menu.getMenu().findItem(R.id.menuDistinguishCommentAsMod);
                modItem.setTitle(R.string.commentRemoveModDistinguish);
            }

            // Set text to "Remove sticky"
            if (comment.getDepth() == 0 && comment.isStickied()) {
                MenuItem modItem = menu.getMenu().findItem(R.id.menuStickyComment);
                modItem.setTitle(R.string.commentRemoveSticky);
            }
        }

        // Default is "Save comment", if comment already is saved, change the text
        if (comment.isSaved()) {
            MenuItem savedItem = menu.getMenu().findItem(R.id.menuSaveOrUnsaveComment);
            savedItem.setTitle(view.getContext().getString(R.string.unsaveComment));
        }

        RedditApi api = App.get().getApi();

        // This response handler will work for any API call updating the distinguish/sticky status of a comment
        OnResponse<RedditComment> distinguishAndStickyResponse = response -> {
            comment.setDistinguished(response.getDistinguished());
            comment.setStickied(response.isStickied());
            adapter.notifyItemChanged(comment);
        };

        menu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menuDeleteComment) {
                // This won't actually return anything valid, so we just assume the comment was deleted
                // This should update the adapter probably?
                api.comment(comment.getId()).delete(response -> Snackbar.make(view, R.string.commentDeleted, LENGTH_SHORT).show(), ((error, t) -> {
                    Util.handleGenericResponseErrors(view, error, t);
                }));
                return true;
            } else if (itemId == R.id.menuSaveOrUnsaveComment) {
                saveCommentOnClick(view, comment);
                return true;
            } else if (itemId == R.id.menuDistinguishCommentAsMod) {
                if (comment.isMod()) {
                    api.comment(comment.getId()).removeDistinguishAsMod(distinguishAndStickyResponse, (e, t) -> {
                        Util.handleGenericResponseErrors(view, e, t);
                    });
                } else {
                    api.comment(comment.getId()).distinguishAsMod(distinguishAndStickyResponse, (e, t) -> {
                        Util.handleGenericResponseErrors(view, e, t);
                    });
                }
            } else if (itemId == R.id.menuStickyComment) {
                if (comment.isStickied()) {
                    api.comment(comment.getId()).removeSticky(distinguishAndStickyResponse, (e, t) -> {
                        Util.handleGenericResponseErrors(view, e, t);
                    });
                } else {
                    api.comment(comment.getId()).sticky(distinguishAndStickyResponse, (e, t) -> {
                        Util.handleGenericResponseErrors(view, e, t);
                    });
                }
            }

            return false;
        });

        menu.show();
    }

    /**
     * Shows the popup for comments for when the comment is NOT posted by the user currently logged in
     *
     * <p>See also: {@link MenuClickHandler#showPopupForCommentExtra(View, RedditComment, CommentsAdapter)}</p>
     *
     * @param view The view clicked (where the popup will be attached)
     * @param comment The comment the popup is for
     * @param adapter The RecyclerView adapter the comment is in
     */
    public static void showPopupForCommentExtraForNonLoggedInUser(View view, RedditComment comment, CommentsAdapter adapter) {
        PopupMenu menu = new PopupMenu(view.getContext(), view);
        menu.inflate(R.menu.comment_extra_generic_for_all_users);
        menu.inflate(R.menu.comment_extra_not_by_user);

        // Default is "Save comment", if comment already is saved, change the text
        if (comment.isSaved()) {
            MenuItem savedItem = menu.getMenu().findItem(R.id.menuSaveOrUnsaveComment);
            savedItem.setTitle(view.getContext().getString(R.string.unsaveComment));
        }

        RedditApi api = App.get().getApi();

        menu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            // TODO a lot of copy&paste code for this and showPopupForCommentExtraForLoggedInUser
            if (itemId == R.id.blockUser) {
                api.user(comment.getAuthor()).block(ignored -> {
                    Snackbar.make(view, R.string.userBlocked, LENGTH_SHORT).show();
                }, (e, t) -> {
                    Util.handleGenericResponseErrors(view, e, t);
                });
                return true;
            } else if (itemId == R.id.menuSaveOrUnsaveComment) {
                saveCommentOnClick(view, comment);
                return true;
            } else if (itemId == R.id.menuShowCommentChain) {
                adapter.setCommentIdChain(comment.getId());
                return true;
            } else if (itemId == R.id.menuCopyCommentLink) {
                // The permalink is only the path
                String url = "https://reddit.com" + comment.getPermalink();

                ClipboardManager clipboard = (ClipboardManager) view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Reddit comment link", url);
                clipboard.setPrimaryClip(clip);

                Toast.makeText(view.getContext(), R.string.linkCopied, Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });

        menu.show();
    }

    /**
     * Convenience method for when "Save comment" or "Unsave comment" has been clicked in a menu.
     *
     * <p>Makes an API request to save or unsave the comment based on the current save state</p>
     *
     * @param view The view clicked (used to attach the snackbar with potential error messages)
     * @param comment The comment to save/unsave. This is updated if the request is successful
     */
    private static void saveCommentOnClick(View view, RedditComment comment) {
        RedditApi api = App.get().getApi();

        if (comment.isSaved()) {
            // Unsave
            api.comment(comment.getId()).unsave(ignored -> {
                comment.setSaved(false);
                Snackbar.make(view, R.string.commentUnsaved, LENGTH_SHORT).show();
            }, (e, t) -> {
                Util.handleGenericResponseErrors(view, e, t);
            });
        } else {
            // Save
            api.comment(comment.getId()).save(ignored -> {
                comment.setSaved(true);
                Snackbar.make(view, R.string.commentSaved, LENGTH_SHORT).show();
            }, (e, t) -> {
                Util.handleGenericResponseErrors(view, e, t);
            });
        }
    }


    /**
     * Shows the extra popup for posts. Based on the user status, a different popup is shown
     * (ie. if the logged in user is the post poster a different popup is shown)
     *
     * @param view The view clicked
     * @param post The post the popup is for
     */
    public static void showPopupForPost(View view, RedditPost post) {
        // If the menu is clicked before the post loads, it will be passed as null
        if (post == null) {
            return;
        }
        RedditUser user = App.getStoredUser();

        if (!App.get().isUserLoggedInPrivatelyBrowsing() && user != null && user.getUsername().equalsIgnoreCase(post.getAuthor())) {
            showPopupForPostExtraForLoggedInUser(view, post);
        } else {
            showPopupForPostExtraForNonLoggedInUser(view, post);
        }
    }

    /**
     * Shows the extra popup for posts for when the post is by the logged in user
     *
     * <p>See also: {@link MenuClickHandler#showPopupForPost(View, RedditPost)}</p>
     *
     * @param view The view clicked (where the popup will be attached)
     * @param post The post the popup is for
     */
    public static void showPopupForPostExtraForLoggedInUser(View view, RedditPost post) {
        PopupMenu menu = new PopupMenu(view.getContext(), view);
        menu.inflate(R.menu.post_extra_generic_for_all_users);
        menu.inflate(R.menu.post_extra_by_user);

        if (post.isUserMod()) {
            menu.inflate(R.menu.post_extra_user_is_mod);

            // Set text to "Undistinguish"
            if (post.isMod()) {
                MenuItem modItem = menu.getMenu().findItem(R.id.menuDistinguishPostAsMod);
                modItem.setTitle(R.string.postRemoveModDistinguish);
            }
            if (post.isStickied()) {
                MenuItem modItem = menu.getMenu().findItem(R.id.menuStickyPost);
                modItem.setTitle(R.string.postRemoveSticky);
            }
        }

        // Default is "Save post", if post already is saved, change the text
        if (post.isSaved()) {
            MenuItem savedItem = menu.getMenu().findItem(R.id.saveOrUnsavePost);
            savedItem.setTitle(view.getContext().getString(R.string.unsavePost));
        }

        RedditApi api = App.get().getApi();

        // This response handler will work for any API call updating the distinguish/sticky status of a post
        // TODO the post this is in should be updated, this will either be a PostsAdapter or the post in PostActivity
        OnResponse<RedditPost> distinguishAndStickyResponse = response -> {
            post.setDistinguished(response.getDistinguished());
            post.setStickied(response.isStickied());
        };

        // TODO these menu calls to the api that update the posts should probably update the local database as well

        menu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            // TODO add delete post

            if (itemId == R.id.saveOrUnsavePost) {
                savePostOnClick(view, post);
                return true;
            } else if (itemId == R.id.menuDistinguishPostAsMod) {
                if (post.isMod()) {
                    api.post(post.getId()).removeDistinguishAsMod(distinguishAndStickyResponse, (e, t) -> {
                        Util.handleGenericResponseErrors(view, e, t);
                    });
                } else {
                    api.post(post.getId()).distinguishAsMod(distinguishAndStickyResponse, (e, t) -> {
                        Util.handleGenericResponseErrors(view, e, t);
                    });
                }
            } else if (itemId == R.id.menuStickyPost) {
                if (post.isStickied()) {
                    api.post(post.getId()).removeSticky(ignored -> {
                        post.setStickied(false);
                    }, (e, t) -> {
                        Util.handleGenericResponseErrors(view, e, t);
                    });
                } else {
                    api.post(post.getId()).sticky(ignored -> {
                        post.setStickied(true);
                    }, (e, t) -> {
                        Util.handleGenericResponseErrors(view, e, t);
                    });
                }
            }

            return false;
        });

        menu.show();
    }

    /**
     * Shows the extra popup for posts for when the post is NOT by the logged in user
     *
     * <p>See also: {@link MenuClickHandler#showPopupForPost(View, RedditPost)}</p>
     *
     * @param view The view clicked (where the popup will be attached)
     * @param post The post the popup is for
     */
    public static void showPopupForPostExtraForNonLoggedInUser(View view, RedditPost post) {
        PopupMenu menu = new PopupMenu(view.getContext(), view);
        menu.inflate(R.menu.post_extra_generic_for_all_users);
        menu.inflate(R.menu.post_extra_not_by_user);

        // Default is "Save post", if post already is saved, change the text
        if (post.isSaved()) {
            MenuItem savedItem = menu.getMenu().findItem(R.id.saveOrUnsavePost);
            savedItem.setTitle(view.getContext().getString(R.string.unsavePost));
        }

        RedditApi api = App.get().getApi();

        menu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.blockUser) {
                api.user(post.getAuthor()).block(ignored -> {
                    Snackbar.make(view, R.string.userBlocked, LENGTH_SHORT).show();
                }, (e, t) -> {
                    Util.handleGenericResponseErrors(view, e, t);
                });
            } else if (itemId == R.id.saveOrUnsavePost) {
                savePostOnClick(view, post);
                return true;
            }

            return false;
        });

        menu.show();
    }

    /**
     * Convenience method for when "Save post" or "Unsave post" has been clicked in a menu.
     *
     * <p>Makes an API request to save or unsave the comment based on the current save state</p>
     *
     * @param view The view clicked (used to attach the snackbar with potential error messages)
     * @param post The post to save/unsave. This is updated if the request is successful
     */
    private static void savePostOnClick(View view, RedditPost post) {
        RedditApi api = App.get().getApi();

        if (post.isSaved()) {
            // Unsave
            api.post(post.getId()).unsave(ignored -> {
                post.setSaved(false);
                Snackbar.make(view, R.string.postUnsaved, LENGTH_SHORT).show();
            }, (e, t) -> {
                Util.handleGenericResponseErrors(view, e, t);
            });
        } else {
            // Save
            api.post(post.getId()).save(ignored -> {
                post.setSaved(true);
                Snackbar.make(view, R.string.postSaved, LENGTH_SHORT).show();
            }, (e, t) -> {
                Util.handleGenericResponseErrors(view, e, t);
            });
        }
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

        boolean isPrivatelyBrowsing = App.get().getApi().isPrivatelyBrowsing();
        if (isPrivatelyBrowsing) {
            MenuItem savedItem = menu.getMenu().findItem(R.id.menuBrowsePrivately);
            savedItem.setTitle(view.getContext().getString(R.string.menuPrivateBrowsingDisable));
        }

        menu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.menuLogOut) {
                // log out etc etc
                App.get().logOut();
                return true;
            } else if (itemId == R.id.menuBrowsePrivately) {
                App.get().enablePrivateBrowsing(!isPrivatelyBrowsing);

                /*
                Fragment f = FragmentManager.findFragment(view);
                if (f instanceof ProfileFragment) {
                    ((ProfileFragment)f).enablePrivateBrowsing(!isPrivatelyBrowsing);
                }
                 */

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
