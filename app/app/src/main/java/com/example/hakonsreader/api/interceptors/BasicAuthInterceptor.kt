package com.example.hakonsreader.api.interceptors

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor that adds the "Authorization: basic " header to a request. This should be used for OAuth related
 * API calls that do not use the access token as the authorization header
 *
 * The interceptor adds the value found in [headerValue]
 */
class BasicAuthInterceptor(private val headerValue: String) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = original.newBuilder()
                .header("Authorization", headerValue)
                .build()
        return chain.proceed(request)
    }
}