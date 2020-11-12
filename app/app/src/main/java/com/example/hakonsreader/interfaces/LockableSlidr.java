package com.example.hakonsreader.interfaces;

/**
 * Interface used for locking a Slidr
 */
public interface LockableSlidr {

    /**
     * Locks or unlocks an activity or fragment that has a {@link com.r0adkll.slidr.Slidr} attached to it
     *
     * @param lock True to lock the Slidr
     */
    void lock(boolean lock);
}
