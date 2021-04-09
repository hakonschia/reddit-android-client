package com.example.hakonsreader.api.model

import com.example.hakonsreader.api.interfaces.*
import com.example.hakonsreader.api.jsonadapters.BooleanAsIntAdapter
import com.example.hakonsreader.api.jsonadapters.EmptyStringAsNullAdapter
import com.example.hakonsreader.api.jsonadapters.NullAsIntAdapter
import com.example.hakonsreader.api.model.flairs.RichtextFlair
import com.example.hakonsreader.api.responses.ListingResponse
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

class RedditComment : RedditListing(),
        VoteableListing,
        ReplyableListing,
        ReportableListing,
        AwardableListing,
        LockableListing,
        DistinguishableListing {

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
    override var author = ""

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
     * True if the comment is locked and cannot be replied to
     */
    @SerializedName("locked")
    override var isLocked = false

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
    var collapsedReason: String? = null


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

    /**
     * The permalink to the comment (eg. /r/GlobalOffensive/comments/postID/title/commentID)
     */
    @SerializedName("permalink")
    var permalink = ""

    /**
     * How the comment is distinguished
     *
     * @see isMod
     * @see isAdmin
     */
    @SerializedName("distinguished")
    override var distinguished: String? = null

    /**
     * True if the comment is made by the poster of the post
     */
    @SerializedName("is_submitter")
    var isByPoster = false

    /**
     * True if the comment is archived (ie. the post the comment is in is archived)
     *
     * Archived comments cannot be voted on or replied to
     */
    @SerializedName("archived")
    override var isArchived = false


    /**
     * The score of the comment
     */
    @SerializedName("score")
    override var score = 0

    /**
     * True if the score should be hidden
     */
    @SerializedName("score_hidden")
    override var isScoreHidden = false

    /**
     * The internal value used for [voteType]
     *
     * True = upvote
     * False = downvote
     * Null = no vote
     */
    @SerializedName("likes")
    override var liked: Boolean? = null

    @SerializedName("all_awardings")
    override var awardings: List<RedditAward>? = null


    /**
     * The user reports on the comment.
     *
     * This will be an array of reports where each report is an array where the first element is a string
     * of the report text, and the second is a number which says something
     */
    @SerializedName("user_reports")
    override var userReports: Array<Array<Any>>? = null

    /**
     * The dismissed user reports on the comment.
     *
     * This will be an array of reports where each report is an array where the first element is a string
     * of the report text, and the second is a number which says something
     */
    @SerializedName("user_reports_dismissed")
    override var userReportsDismissed: Array<Array<Any>>? = null

    /**
     * The amount of reports the comment has
     */
    @JsonAdapter(NullAsIntAdapter::class)
    @SerializedName("num_reports")
    override var numReports = 0

    /**
     * True if reports are set to be ignored on the comment
     */
    @SerializedName("ignore_reports")
    override var ignoreReports = false

    /**
     * The timestamp the post was edited. If this is negative the post hasn't been edited
     */
    @JsonAdapter(BooleanAsIntAdapter::class)
    @SerializedName("edited")
    var edited = -1


    @SerializedName("replies")
    // No replies are represented as: {"replies": ""} which would cause an error since it's a string
    @JsonAdapter(EmptyStringAsNullAdapter::class)
    private var repliesInternal: ListingResponse<RedditComment>? = null

    /**
     * The actual replies for the comment. If this is *null* then [replies] has not yet been accessed
     */
    private var repliesActual: ArrayList<RedditComment>? = null

    /**
     * The list of replies this comment has
     */
    // This has to be transient as GSON would clash between this and repliesInternal, since it is
    // marked with "replies" as its serialized name
    @Transient
    var replies = ArrayList<RedditComment>()
        get() {
            if (repliesActual == null) {
                repliesActual = if (repliesInternal != null) {
                    getRepliesInternal() as ArrayList<RedditComment>
                } else {
                    ArrayList()
                }
            }

            return repliesActual as ArrayList<RedditComment>
        }


    private fun getRepliesInternal(): List<RedditComment> {
        // All the comments from the current and its replies
        val all = ArrayList<RedditComment>()

        // Loop through the list of replies and add the reply and the replies to the reply
        repliesActual = repliesInternal?.getListings() as ArrayList<RedditComment>
        for (reply in repliesActual!!) {
            all.add(reply)
            all.addAll(reply.replies)
        }

        return all
    }

    /**
     * Removes a reply from the comment
     *
     *
     * The reply must be a direct child
     *
     * @param reply The reply to remove
     */
    fun removeReply(reply: RedditComment?) {
        replies.remove(reply)
    }

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
    fun addReplies(replies: List<RedditComment>) {
        // Add all as replies to this comment
        this.replies.addAll(replies)

        // Each comment holds a list of the replies to itself, so for every reply add the rest of
        // the comment chain as a reply to it
        for (i in replies.indices) {
            val reply = replies[i]

            // Create the chain of this replies comments and add them
            val replyChain = createCommentChain(replies, i)
            reply.replies.addAll(replyChain)
        }
    }

    /**
     * Creates a subchain of comments
     *
     * @param parentChain The parent chain to create a subchain from
     * @param pos The position of `parentChain` to start at
     * @return The comment chain after `pos`
     */
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
}