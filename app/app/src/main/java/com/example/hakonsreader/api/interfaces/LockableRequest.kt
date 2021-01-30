package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.responses.ApiResponse

interface LockableRequest {
    suspend fun lock() : ApiResponse<Any?>
    suspend fun unlock() : ApiResponse<Any?>
}