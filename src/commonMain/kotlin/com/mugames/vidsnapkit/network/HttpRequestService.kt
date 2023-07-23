/*
 *    Copyright (c) 2023 Udhayarajan M
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.mugames.vidsnapkit.network

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.statement.*
import java.util.*

/**
 * @author Udhaya
 * Created on 21-01-2022
 */
interface HttpRequestService {
    suspend fun getResponse(
        url: String,
        headers:
        Hashtable<String, String>? = null,
    ): String?

    /**
     * Makes Http request
     *
     * @return HttpResponse
     */
    suspend fun getRawResponse(
        url: String,
        headers: Hashtable<String, String>? = null,
        followRedirect: Boolean = true,
    ): HttpResponse?

    /**
     * Used to estimate size of given url in bytes
     *
     * @return bytes count of given [url]
     */
    suspend fun getSize(
        url: String,
        headers: Hashtable<String, String>? = null,
    ): Long

    suspend fun postRequest(
        url: String,
        headers: Hashtable<String, String>? = null,
        postData: Hashtable<String, Any>? = null,
    ): String

    suspend fun postRawResponse(
        url: String,
        headers: Hashtable<String, String>? = null,
        postData: Hashtable<String, Any>? = null,
        followRedirect: Boolean = true,
    ): HttpResponse?

    suspend fun checkPageAvailability(
        url: String,
        headers: Hashtable<String, String>? = null,
    ): Boolean

    fun close()

    companion object {
        fun create(
            client: HttpClient = HttpClient(Android),
            storage: CookiesStorage? = null,
        ): HttpRequestService =
            HttpRequestServiceImpl(client.config {
                install(HttpTimeout) {
                    socketTimeoutMillis = 13_000
                    requestTimeoutMillis = 13_000
                    connectTimeoutMillis = 13_000
                }
                install(HttpCookies) {
                    if (storage != null) {
                        this.storage = storage
                    }
                }
            })
    }
}
