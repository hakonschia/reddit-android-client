package com.example.hakonsreader.interfaces

/**
 * Interface that classes that wants to be notified about private browsing changes will implement
 */
interface PrivateBrowsingObservable {

    /**
     * Called when the private browsing state has changed
     *
     * @param privatelyBrowsing True if private browsing is now enabled, false if disabled
     */
    fun privateBrowsingStateChanged(privatelyBrowsing: Boolean)

}