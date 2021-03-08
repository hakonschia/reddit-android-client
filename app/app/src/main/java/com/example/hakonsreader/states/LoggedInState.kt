package com.example.hakonsreader.states

import com.example.hakonsreader.api.model.RedditUserInfo

sealed class LoggedInState {

    /**
     * No user logged in
     */
    object LoggedOut : LoggedInState()

    /**
     * There is a logged in user, browsing normally
     */
    class LoggedIn(val userInfo: RedditUserInfo) : LoggedInState()

    /**
     * There is a logged in user, but the user is currently browsing privately
     */
    class PrivatelyBrowsing(val userInfo: RedditUserInfo) : LoggedInState()

}