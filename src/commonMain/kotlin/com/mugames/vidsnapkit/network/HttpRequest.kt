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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * @author Udhaya
 * Created on 21-01-2022
 */

/**
 * Used to make HTTP request
 *
 * @param url Endpoint URL to make HTTP request
 * @param headers Key and Value a pair of Header for HTTP request(optional).
 */
class HttpRequest(
    private val url: String,
    private val headers: Hashtable<String, String>? = null,
) {
    private companion object {
        fun createClient(): HttpInterface {
            return HttpInterfaceImpl(HttpClient(Android) {
                followRedirects = true
            })
        }
    }

    /**
     * Fetches plain text for given url
     *
     * @return Text format of entire webpage for given [url]
     */
    suspend fun getResponse(): String = withContext(Dispatchers.IO) { createClient().getData(url, headers) }

    /**
     * Used to estimate size of given url in bytes
     *
     * @return bytes count of given [url]
     */
    suspend fun getSize() = createClient().getSize(url)
}
