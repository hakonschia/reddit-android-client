package com.example.hakonsreader.api.interfaces

import com.example.hakonsreader.api.responses.ApiResponse

interface ReportableRequest {

    suspend fun ignoreReports() : ApiResponse<Any?>

    suspend fun unignoreReports() : ApiResponse<Any?>

}