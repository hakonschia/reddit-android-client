package com.example.hakonsreader.viewmodels

import com.example.hakonsreader.api.responses.GenericError

/**
 * Wrapper for errors given by the API to communicate from a ViewModel to its fragment/activity
 */
class ErrorWrapper(
        /**
         * @return The GenericError
         */
        val error: GenericError,
        /**
         * @return The throwable for the error
         */
        val throwable: Throwable)