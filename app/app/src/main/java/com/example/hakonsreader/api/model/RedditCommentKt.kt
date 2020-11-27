package com.example.hakonsreader.api.model

import com.example.hakonsreader.api.model.flairs.RichtextFlair
import com.google.gson.annotations.SerializedName

class RedditCommentKt : RedditListingKt() {

    /**
     * The body of comment in Markdown
     *
     * @see bodyHtml
     */
    @SerializedName("body")
    var body = ""

    /**
     * The body of the comment in HTML
     *
     * @see body
     */
    @SerializedName("body_html")
    var bodyHtml = ""

    /**
     * The author of the comment
     */
    @SerializedName("author")
    var author = ""

    /**
     * The fullname of the author
     */
    @SerializedName("author_fullname")
    var authorFullname = ""

    /**
     * The fullname of the comments parent. For top-level comments this will be the fullname of
     * the post the comment is in
     */
    @SerializedName("parent_id")
    var parentId = ""

    /**
     * The fullname of the Subreddit the comment is in
     */
    @SerializedName("subreddit_id")
    var subredditId = ""

    /**
     * The name of the Subreddit the comment is in
     */
    @SerializedName("subreddit")
    var subreddit = ""


    /**
     * The depth of the comment
     */
    @SerializedName("depth")
    var depth = 0


    /**
     * True if the currently logged in user has saved the comment
     */
    @SerializedName("saved")
    var isSaved = false

    /**
     * True if the currently logged in user is a moderator in the Subreddit the comment is in
     */
    @SerializedName("can_mod_post")
    var isUserMod = false

    /**
     * True if the comment has been edited after it was posted
     */
    @SerializedName("edited")
    var isEdited = false

    /**
     * True if the comment is locked and cannot be replied to
     */
    @SerializedName("locked")
    var isLocked = false

    /**
     * True if the comment is stickied
     */
    @SerializedName("stickied")
    var isStickied = false

    /**
     * True if Reddit has marked this comment as collapsed (ie. it should be hidden by default)
     *
     * @see collapsedReason
     */
    @SerializedName("collapsed")
    var isCollapsed = false

    /**
     * The reason the comment is collapsed
     *
     * @see isCollapsed
     */
    @SerializedName("collapsed_reason")
    // TODO this can be null in the response, should maybe be "String?"
    var collapsedReason = ""


    /**
     * For when a comment is a "4 more comments" comment, this holds the amount of extra comments
     * that can be loaded
     *
     * @see children
     */
    @SerializedName("count")
    var extraCommentsCount = 0

    /**
     * For when a comment is a "4 more comments" comments, this holds the IDs of the comments that
     * can be loaded
     */
    @SerializedName("children")
    var children = ArrayList<String>()


    /**
     * The hex color of the background of the authors flair
     *
     * This might be the string "transparent" for transparent backgrounds
     */
    @SerializedName("author_flair_background_color")
    var authorFlairBackgroundColor = ""

    /**
     * The text color for the authors flair
     */
    @SerializedName("author_flair_text_color")
    var authorFlairTextColor = ""

    /**
     * The text for the authors flair
     */
    @SerializedName("author_flair_text")
    var authorFlairText = ""

    /**
     * The list of [RichtextFlair] the authors flair is combined of
     */
    @SerializedName("author_flair_richtext")
    var authorRichtextFlairs = ArrayList<RichtextFlair>()


    // TODO when everything is converted and ListingResponseKt takes T : RedditListingKt then do this
    /*
    @SerializedName("replies")
    // No replies are represented as: {"replies": ""} which would cause an error since it's a string
    @JsonAdapter(EmptyStringAsNullAdapter::class)
    private var repliesInternal: ListingResponseKt<RedditComment>? = null
  //  var ListingResponseKt: ListingResponseKt<RedditCommentKt>? = null

    var replies = ArrayList<RedditComment>()
     */

    /**
     * Retrieves the list of replies this comment has
     *
     * @return The list of replies
     */
    /*
    fun getReplies(): List<RedditComment?>? {
        // First time retrieving replies, create the list from data.replies
        if (repliesActual == null) {
            if (repliesInternal != null) {
                repliesActual = getRepliesInternal()
            } else {
                // No more for the comment
                repliesActual = java.util.ArrayList<RedditComment>()
            }
        }
        return repliesActual
    }

    private fun getRepliesInternal(): List<RedditComment> {
        // All the comments from the current and its replies
        val all: MutableList<RedditComment> = java.util.ArrayList()

        // Loop through the list of replies and add the reply and the replies to the reply
        repliesActual = repliesInternal!!.getListings() as List<RedditComment?>?
        for (reply in repliesActual) {
            all.add(reply)
            all.addAll(reply.replies)
        }
        return all
    }
     */

    /**
     * Removes a reply from the comment
     *
     *
     * The reply must be a direct child
     *
     * @param reply The reply to remove
     */
    /*
    fun removeReply(reply: RedditComment?) {
        repliesActual.remove(reply)
    }
     */

    /**
     * Adds a list of comments as replies. This sets the replies for every comment downwards in the chain
     * so every child has a list to its replies
     *
     *
     * Use this after retrieving new comments via [RedditComment.getChildren]
     * to add the replies. Note that this function should be called on the parent of the comment
     * [RedditComment.getChildren] was called on, as the comments received are replies to the
     * parent, not that object itself
     */
    /*
    fun addReplies(replies: List<RedditComment>?) {
        var replies = replies
        if (replies == null) {
            replies = java.util.ArrayList()
        }

        // Add all as replies to this comment
        this.repliesActual.addAll(replies)

        // Each comment holds a list of the replies to itself, so for every reply add the rest of
        // the comment chain as a reply to it
        for (i in replies.indices) {
            val reply = replies[i]

            // Create the chain of this replies comments and add them
            val replyChain = createCommentChain(replies, i)
            if (reply.repliesActual == null) {
                reply.repliesActual = java.util.ArrayList()
            }
            reply.repliesActual.addAll(replyChain)
        }
    }
    */

    /**
     * Creates a subchain of comments
     *
     * @param parentChain The parent chain to create a subchain from
     * @param pos The position of `parentChain` to start at
     * @return The comment chain after `pos`
     */
    /*
    private fun createCommentChain(parentChain: List<RedditComment>, pos: Int): List<RedditComment> {
        val chain: MutableList<RedditComment> = java.util.ArrayList()
        val start = parentChain[pos]
        for (i in pos + 1 until parentChain.size) {
            val current = parentChain[i]
            if (current.depth > start.depth) {
                chain.add(current)
            } else {
                break
            }
        }
        return chain
    }
    */
}