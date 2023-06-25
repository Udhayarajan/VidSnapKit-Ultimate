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
import io.ktor.client.request.*
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
    companion object {
        private var prefixUrl = ""
        private var additionHeader: Hashtable<String, String>? = null
        private fun defaultClient(requiresRedirection: Boolean = true) = HttpClient(Android).also {
            it.config {
                followRedirects = requiresRedirection
            }
        }.run {
            HttpInterfaceImpl(this)
        }

        private var clientGenerator: () -> HttpClient = {
            HttpClient(Android)
        }

        private fun createClient(requiresRedirection: Boolean = true): HttpInterface {
            return HttpInterfaceImpl(clientGenerator().also { it.config { followRedirects = requiresRedirection } })
        }

        /**
         * @param clientGenerator custom client with custom modification. Note: `followRedirects` will be ignored
         * @param prefixUrl is used when you need to forward requests to a proxy API.
         * The prefixUrl may include the API key and the hostname of the proxy provider.
         * @param headers is used when you may need to provide username:password auth to the requests when using proxy,
         * So you can add auth header to every request that will be sent via your proxy provider
         */
        fun setClient(
            clientGenerator: () -> HttpClient,
            prefixUrl: String = "",
            headers: Hashtable<String, String>? = null
        ) {
            this.prefixUrl = prefixUrl
            this.clientGenerator = clientGenerator
            additionHeader = headers
        }
    }

    private fun getUrl() = prefixUrl + url

    private fun getHeader(): Hashtable<String, String>? {
        val result = Hashtable<String, String>()
        additionHeader?.let { result.putAll(it) }
        headers?.let { result.putAll(it) }
        return result.ifEmpty { null }
    }

    /**
     * Fetches plain text for given url
     *
     * @return Text format of entire webpage for given [url]
     */
    suspend fun getResponse(needsRedirection: Boolean = true, useCustomClient: Boolean = true): String? =
        withContext(Dispatchers.IO) {
            (if (useCustomClient) createClient(needsRedirection) else defaultClient()).getData(
                getUrl(),
                getHeader()
            )
        }

    /**
     * Used to estimate size of given url in bytes
     *
     * @return bytes count of given [url]
     */
    suspend fun getSize(useCustomClient: Boolean = true) =
        (if (useCustomClient) createClient() else defaultClient()).getSize(url)

    suspend fun postRequest(postData: Hashtable<String, Any>? = null, useCustomClient: Boolean = true): String =
        withContext(Dispatchers.IO) {
            (if (useCustomClient) createClient() else defaultClient()).postData(
                getUrl(),
                postData,
                getHeader()
            )
        }

    suspend fun getRawResponse(useCustomClient: Boolean = true) =
        (if (useCustomClient) createClient() else defaultClient()).getRawResponse(getUrl(), getHeader())

    suspend fun isAvailable(useCustomClient: Boolean = true): Boolean =
        withContext(Dispatchers.IO) {
            (if (useCustomClient) createClient() else defaultClient(false)).checkWebPage(
                getUrl(),
                getHeader()
            )
        }

}
