package com.example.hakonsreader.interfaces

import com.example.hakonsreader.api.enums.PostTimeSort
import com.example.hakonsreader.api.enums.SortingMethods

/**
 * Interface for fragments/activities that have some sort of sorting (eg. posts that can be sorted
 * by new/hot/top etc.) that are also able to be sorted by time (eg. top of all time)
 */
interface SortableWithTime {
    /**
     * Called when something should be sorted by new
     */
    fun new()

    /**
     * Called when something should be sorted by hot
     */
    fun hot()

    /**
     * Called when something should be sorted by top
     *
     * @param timeSort The time sort to sort by
     */
    fun top(timeSort: PostTimeSort)

    /**
     * Called when something should be sorted by controversial
     *
     * @param timeSort The time sort to sort by
     */
    fun controversial(timeSort: PostTimeSort)

    /**
     * Gets the current sort
     */
    fun currentSort() : SortingMethods

    /**
     * Gets the current time sort
     *
     * @return A PostTimeSort, or null if the current sort doesn't have a time sort
     */
    fun currentTimeSort() : PostTimeSort?
}