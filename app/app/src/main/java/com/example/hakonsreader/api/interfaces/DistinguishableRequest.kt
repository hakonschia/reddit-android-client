package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.responses.ApiResponse

interface DistinguishableRequest {
    suspend fun distinguishAsMod() : ApiResponse<DistinguishableListing>
    suspend fun removeModDistinguish() : ApiResponse<DistinguishableListing>
}