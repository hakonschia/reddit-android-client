package com.example.hakonsreader.api.interceptors;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;

/**
 * Interceptor that adds the User-Agent header to the request
 */
public class UserAgentInterceptor implements Interceptor {

    private final String userAgent;

    /**
     * @param userAgent The user agent to add to the requests
     */
    public UserAgentInterceptor(String userAgent) {
        this.userAgent = userAgent;
    }

    @NotNull
    @Override
    public okhttp3.Response intercept(Interceptor.Chain chain) throws IOException {
        Request original = chain.request();
        Request request = original.newBuilder()
                .header("User-Agent", userAgent)
                .build();

        return chain.proceed(request);
    }
}