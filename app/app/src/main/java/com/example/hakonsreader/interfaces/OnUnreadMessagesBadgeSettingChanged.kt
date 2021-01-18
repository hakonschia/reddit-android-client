package com.example.hakonsreader.interfaces

/**
 * Interface for when the setting for displaying a badge of unread messages has changed
 */
fun interface OnUnreadMessagesBadgeSettingChanged {

    /**
     * @param show True if the badge should now be shown
     */
    fun showUnreadMessagesBadge(show: Boolean)

}