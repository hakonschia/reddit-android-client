package com.example.hakonsreader.interfaces

/**
 * Interface for classes listening to reports being set to ignored/unignored
 */
interface OnReportsIgnoreChangeListener {

    /**
     * Called when reports ignored status has been updated
     *
     * @param ignored True if reports have been set to "Ignored", false for "Unignored"
     */
    fun onIgnoredChange(ignored: Boolean)
    
}