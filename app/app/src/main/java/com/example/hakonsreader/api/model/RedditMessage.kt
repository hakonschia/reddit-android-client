package com.example.hakonsreader.api.model

import androidx.room.Ignore
import com.example.hakonsreader.api.enums.RedditMessageType
import com.google.gson.annotations.SerializedName

/**
 * Class representing a Reddit message. This class handles both inbox messages and private messages
 */
class RedditMessage : RedditListing() {

    /**
     * The author of the message
     */
    @SerializedName("author")
    var author = ""

    /**
     * The body of message in Markdown
     *
     * @see bodyHtml
     */
    @SerializedName("body")
    var body = ""

    /**
     * The body of the message in HTML
     *
     * @see body
     */
    @SerializedName("body_html")
    var bodyHtml = ""

    /**
     * True if the message is in an inbox and stemming from a comment
     */
    @SerializedName("was_comment")
    var wasComment = false

    /**
     * True if the comment is new, ie. is not seen from the inbox yet
     */
    @SerializedName("new")
    var isNew = false

    /**
     * The score of the comment (if applicable, or 0 otherwise)
     */
    @SerializedName("score")
    var score = 0

    /**
     * The subreddit the comment is posted in (if applicable, this will be `null` if not posted
     * in a subreddit)
     */
    @SerializedName("subreddit")
    var subreddit: String? = null

    /**
     * The subject of the message, if this is a private message.
     *
     * If this is a comment reply, it will be "kommentarsvar" if your user is norwegian :)
     */
    @SerializedName("subject")
    var subject = ""

    /**
     * The context for the message. For comment replies this be the path to the comment (ie. /r/subreddit ...)
     */
    @SerializedName("context")
    var context = ""

    /**
     * The internal value of the type, as used by [type]
     */
    @SerializedName("type")
    private var typeInternal = ""

    /**
     * What type of message this is
     */
    val type: RedditMessageType by lazy {
        when (typeInternal) {
            "comment_reply" -> RedditMessageType.COMMENT_REPLY
            else -> RedditMessageType.UNKNOWN
        }
    }


    /**
     * How the message is distinguished
     *
     * @see isMod
     * @see isAdmin
     */
    @SerializedName("distinguished")
    var distinguished: String? = null

    /**
     * @return True if the message is made by, and distinguished as, a moderator
     * @see distinguished
     */
    fun isMod() : Boolean = distinguished == "moderator"

    /**
     * @return True if the message is made by, and distinguished as, an admin (Reddit employee)
     * @see distinguished
     */
    fun isAdmin() : Boolean = distinguished == "admin"
}