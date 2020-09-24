package com.example.hakonsreader.api.interfaces;

/**
 * Interface for all common attributes for a Reddit listing
 *
 * <p>There are also other types of mores specific listings, such as {@link VotableListing}</p>
 */
public interface RedditListing {

    /**
     * @return What kind of listing this is
     */
    String getKind();

    /**
     * @return The ID of the listing
     */
    String getID();


    /**
     * @return The URL of the listing
     */
    String getURL();

    /**
     * @return The fullname of the listing (equivalent to "{@link RedditListing#getKind()} + "_" + {@link RedditListing#getID()})
     */
    String getFullname();

    /**
     * @return The unix timestamp in milliseconds when the listing was created
     */
    long getCreatedAt();

    /**
     * @return True if the listing is NSFW (over 18)
     */
    boolean isNSFW();

}
