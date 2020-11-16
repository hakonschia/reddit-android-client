package com.example.hakonsreader.interfaces;

import com.example.hakonsreader.api.enums.PostTimeSort;

/**
 * Interface for fragments/activities that have some sort of sorting (eg. posts that can be sorted
 * by new/hot/top etc.) that are also able to be sorted by time (eg. top of all time)
 */
public interface SortableWithTime {

    /**
     * Called when something should be sorted by new
     */
    void newSort();

    /**
     * Called when something should be sorted by hot
     */
    void hot();

    /**
     * Called when something should be sorted by top
     *
     * @param timeSort The time sort to sort by
     */
    void top(PostTimeSort timeSort);

    /**
     * Called when something should be sorted by controversial
     *
     * @param timeSort The time sort to sort by
     */
    void controversial(PostTimeSort timeSort);

}
