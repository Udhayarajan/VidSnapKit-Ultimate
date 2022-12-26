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
