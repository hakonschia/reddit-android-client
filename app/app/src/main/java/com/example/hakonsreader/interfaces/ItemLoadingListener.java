package com.example.hakonsreader.interfaces;

/**
 * Interface for when this fragment starts/finishes loading something
 */
public interface ItemLoadingListener {

    /**
     * Called when an item has started/finished loading
     *
     * @param up If true, an item has started loading. If false an item has finished loading
     */
    void onCountChange(boolean up);
}