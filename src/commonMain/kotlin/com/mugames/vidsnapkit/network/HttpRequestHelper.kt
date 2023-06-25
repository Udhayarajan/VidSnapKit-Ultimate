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

import com.mugames.vidsnapkit.toJsonString
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Pattern

/**
 * @author Udhaya
 * Created on 21-01-2022
 */
interface HttpInterface {
    suspend fun getData(url: String, headers: Hashtable<String, String>? = null): String?
    suspend fun getRawResponse(url: String, headers: Hashtable<String, String>? = null): HttpResponse
    suspend fun getSize(url: String, headers: Hashtable<String, String>? = null): Long

    suspend fun postData(
        url: String,
        postData: Hashtable<String, Any>? = null,
        headers: Hashtable<String, String>? = null
    ): String

    suspend fun checkWebPage(url: String, headers: Hashtable<String, String>?): Boolean
}

class HttpInterfaceImpl(
    private val client: HttpClient,
) : HttpInterface {

    private val redirectionStatusCode = setOf(
        HttpStatusCode.MovedPermanently,
        HttpStatusCode.Found,
        HttpStatusCode.TemporaryRedirect
    )

    companion object {
        private val logger = LoggerFactory.getLogger(HttpInterfaceImpl::class.java)
    }

    override suspend fun postData(
        url: String,
        postData: Hashtable<String, Any>?,
        headers: Hashtable<String, String>?
    ): String {
        return try {
            client.post {
                url(url)
                headers?.let {
                    if (it.isNotEmpty())
                        headers {
                            for ((key, value) in it)
                                append(key, value)
                        }
                }
                postData?.let {
                    setBody(TextContent(it.toJsonString(), ContentType.Application.Json))
                }
            }.bodyAsText()
        } catch (e: Error) {
            logger.error(
                "postData() url=${url} header=${headers.toString()} & postData=${postData.toString()} Error:",
                e
            )
            throw e
        }
    }

    // Instagram Server crashes with 500 if we sent wrong cookies
    // So it is tackled by hardcoding and making it as true to prevent NonFatal Error
    override suspend fun checkWebPage(url: String, headers: Hashtable<String, String>?): Boolean {
        val acceptedStatusCode = setOf(
            HttpStatusCode.OK,
            HttpStatusCode.Accepted,
            HttpStatusCode.Created,
            HttpStatusCode.NonAuthoritativeInformation,
            HttpStatusCode.NoContent,
            HttpStatusCode.PartialContent,
            HttpStatusCode.ResetContent,
            HttpStatusCode.MultiStatus,
            if (url.contains("instagram")) HttpStatusCode.InternalServerError else HttpStatusCode.OK
        )

        return try {
            client.get {
                url(url)
                method = HttpMethod.Head
                headers?.let {
                    if (it.isNotEmpty())
                        headers {
                            for ((key, value) in it)
                                append(key, value)
                        }
                }
            }.run {
                status in acceptedStatusCode || run {
                    if (status in redirectionStatusCode) {
                        val res = getLastPossibleRedirectedResponse(this, headers)
                        return res.status in acceptedStatusCode || res.status in redirectionStatusCode
                    }
                    logger.warn("Unhandled in checkWebPage() status code=${status} for url=${url} with headers=${headers.toString()} & response=${bodyAsText()}")
                    false
                }
            }
        } catch (e: ClientRequestException) {
            logger.error("checkWebPage() url=${url} header=${headers.toString()} ClientRequestException:", e)
            false
        }
    }

    override suspend fun getData(url: String, headers: Hashtable<String, String>?): String? {
        return try {
            client.get {
                url(url)
                headers?.let {
                    if (it.isNotEmpty()) {
                        headers {
                            for ((key, value) in it)
                                append(key, value)
                        }
                    }
                }
            }.run {
                if (status == HttpStatusCode.OK)
                    body()
                else if (status in redirectionStatusCode) {
                    getLastPossibleRedirectedResponse(this, headers).body()
                } else if (url.contains("instagram") && status == HttpStatusCode.InternalServerError) "{error:\"Invalid Cookies\"}"
                else {
                    logger.warn("Unhandled in getData() status code=${status} for url=${url} with headers=${headers.toString()} & response=${bodyAsText()}")
                    null
                }
            }
        } catch (e: ClientRequestException) {
            logger.error("getData() url=${url} header=${headers.toString()} ClientRequestException:", e)
            null
        } catch (e: SendCountExceedException) {
            if (url.contains("instagram") && headers?.containsKey("Cookie") == true)
                "{error:\"Invalid Cookies\"}"
            else throw e
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getRawResponse(url: String, headers: Hashtable<String, String>?): HttpResponse {
        return client.get {
            url(url)
            headers?.let {
                if (it.isNotEmpty()) {
                    headers {
                        for ((key, value) in it)
                            append(key, value)
                    }
                }
            }
        }
    }

    override suspend fun getSize(url: String, headers: Hashtable<String, String>?): Long {
        return client.request {
            method = HttpMethod.Head
            url(url)
        }.run {
            if (status == HttpStatusCode.OK)
                this.headers["content-length"]?.toLong() ?: Long.MIN_VALUE
            else Long.MIN_VALUE
        }
    }

    private suspend fun getLastPossibleRedirectedResponse(
        response: HttpResponse,
        headers: Hashtable<String, String>?
    ): HttpResponse {
        var cnt = 0
        var cacheResponse = response
        do {
            var locationUrl = cacheResponse.headers[HttpHeaders.Location]!!

            val matcher = Pattern.compile("^(?:https?:\\/\\/)?(?:[^@\\n]+@)?(?:www\\.)?([^:\\/\\n?]+)")
                .matcher(locationUrl)
            if (!matcher.find())
                locationUrl = cacheResponse.request.url.protocolWithAuthority + locationUrl
            val nonRedirectingClient = client.config {
                followRedirects = false
            }
            val tempResponse = nonRedirectingClient.get(locationUrl) {
                this.headers {
                    headers?.let {
                        for ((key, value) in it)
                            append(key, value)
                    }
                }
            }
            if (cacheResponse.request.url == tempResponse.request.url)
                break
            cacheResponse = tempResponse
            cnt++
        } while (cacheResponse.status in redirectionStatusCode || cnt < 20)
        return if (cacheResponse.request.url.host == "localhost") response else cacheResponse
    }
}
