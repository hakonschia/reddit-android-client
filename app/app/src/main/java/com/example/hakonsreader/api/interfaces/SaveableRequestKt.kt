package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.responses.ApiResponse

/**
 * Interface for requests that offer functionality to save/unsave a post or comment
 */
interface SaveableRequestKt {

    suspend fun save() : ApiResponse<Nothing?>

    suspend fun unsave() : ApiResponse<Nothing?>
}