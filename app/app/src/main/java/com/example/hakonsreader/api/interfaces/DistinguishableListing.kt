package com.example.hakonsreader.api.interfaces

interface DistinguishableListing {

    var distinguished: String?

    /**
     * @return True if the listing is made by, and distinguished as, an admin (Reddit employee)
     * @see distinguished
     */
    fun isAdmin() = distinguished == "admin"

    /**
     * @return True if the listing is made by, and distinguished as, a moderator
     * @see distinguished
     */
    fun isMod() = distinguished == "moderator"

}
