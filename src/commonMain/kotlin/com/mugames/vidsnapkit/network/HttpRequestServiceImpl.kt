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
import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.TimeoutCancellationException
import org.slf4j.LoggerFactory
import java.net.SocketException
import java.util.*
import java.util.concurrent.CancellationException
import java.util.regex.Pattern
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import kotlin.math.min

/**
 * @author Udhaya
 * @Created 22-07-2023
 */

class HttpRequestServiceImpl(private val client: HttpClient) : HttpRequestService {

    private val redirectionStatusCode = setOf(
        HttpStatusCode.MovedPermanently, HttpStatusCode.Found, HttpStatusCode.TemporaryRedirect
    )

    companion object {
        private val logger = LoggerFactory.getLogger(HttpRequestServiceImpl::class.java)
    }

    override suspend fun getResponse(
        url: String,
        headers: Hashtable<String, String>?,
    ): String? = try {
        client.get {
            url(url)
            headers?.let {
                if (it.isNotEmpty()) {
                    headers {
                        for ((key, value) in it) append(key, value)
                    }
                }
            }
        }.run {
            if (status == HttpStatusCode.OK) {
                bodyAsText()
            } else if (status in redirectionStatusCode) {
                getLastPossibleRedirectedResponse(this, headers).bodyAsText()
            } else if (url.contains("instagram") && status == HttpStatusCode.InternalServerError) {
                "{error:\"Invalid Cookies\"}"
            } else if (status == HttpStatusCode.TooManyRequests) {
                logger.warn("Unhandled in getData() TooManyRequest for url=$url with headers=$headers & response=${bodyAsText()}")
                "429"
            } else {
                val body = bodyAsText()
                logger.warn(
                    "Unhandled in getData() status code=$status for url=$url with headers=$headers &\n response=${
                    body.substring(
                        min(body.length, 2000)
                    )
                    }"
                )
                null
            }
        }
    } catch (e: ClientRequestException) {
        logger.error("getData() url=$url header=$headers ClientRequestException:", e)
        null
    } catch (e: SSLPeerUnverifiedException) {
        logger.error("getData() url=$url header=$headers SSLPeerUnverifiedException ${e.message}")
        throw ProxyException(e)
    } catch (e: SocketTimeoutException) {
        logger.error("getData() url=$url header=$headers SocketTimeoutException ${e.message}")
        throw ProxyException(e)
    } catch (e: SocketException) {
        logger.error("getData() url=$url header=$headers SocketException ${e.message}")
        throw ProxyException(e)
    } catch (e: SSLHandshakeException) {
        logger.error("getData() url=$url header=$headers SLLHandShakeException ${e.message}")
        throw ProxyException(e)
    } catch (e: SendCountExceedException) {
        if (url.contains("instagram") && headers?.containsKey("Cookie") == true) {
            "{error:\"Invalid Cookies\"}"
        } else {
            logger.error("getData() url=$url header=$headers SendCountExceedException:", e)
            throw e
        }
    } catch (e: Exception) {
        if (e is TimeoutCancellationException || e is CancellationException) logger.error("getData() url=$url header=$headers Cancellation exception: ${e.message}")
        else logger.error("getData() url=$url header=$headers Generic exception:", e)
        throw e
    }

    override suspend fun getRawResponse(
        url: String,
        headers: Hashtable<String, String>?,
        followRedirect: Boolean,
    ): HttpResponse? = try {
        var cache = true
        client.config {
            cache = this.followRedirects
            this.followRedirects = followRedirect
        }
        val response = client.get {
            url(url)
            headers?.let {
                if (it.isNotEmpty()) {
                    headers {
                        for ((key, value) in it) append(key, value)
                    }
                }
            }
        }
        client.config {
            this.followRedirects = cache
        }
        response
    } catch (e: SSLPeerUnverifiedException) {
        logger.error("getRawResponse() url=$url header=$headers SSLPeerUnverifiedException ${e.message}")
        throw ProxyException(e)
    } catch (e: SocketTimeoutException) {
        logger.error("getRawResponse() url=$url header=$headers SocketTimeoutException ${e.message}")
        throw ProxyException(e)
    } catch (e: SocketException) {
        logger.error("getRawResponse() url=$url header=$headers SocketException ${e.message}")
        throw ProxyException(e)
    } catch (e: SSLHandshakeException) {
        logger.error("getRawResponse() url=$url header=$headers SLLHandShakeException ${e.message}")
        throw ProxyException(e)
    } catch (e: Exception) {
        if (e is TimeoutCancellationException || e is CancellationException) logger.error("getRawResponse() url=$url header=$headers Cancellation exception: ${e.message}")
        else logger.error("getRawResponse() url=$url header=$headers Generic exception:", e)
        null
    }

    override suspend fun getSize(url: String, headers: Hashtable<String, String>?) = try {
        client.request {
            method = HttpMethod.Head
            url(url)
            headers?.let {
                if (it.isNotEmpty()) headers {
                    for ((key, value) in it) append(key, value)
                }
            }
        }.run {
            if (status == HttpStatusCode.OK) {
                this.headers["content-length"]?.toLong() ?: Long.MIN_VALUE
            } else {
                Long.MIN_VALUE
            }
        }
    } catch (e: Exception) {
        when (e) {
            is HttpRequestTimeoutException,
            is ConnectTimeoutException,
            is SocketTimeoutException,

            is SSLPeerUnverifiedException,
            is SocketException,
            is SSLHandshakeException,
            -> {
                // handle the exception from caller
                1
            }

            else -> {
                logger.error("unhandled exception with calculating size" + e.message)
                throw e
            }
        }
    }

    override suspend fun postRequest(
        url: String,
        headers: Hashtable<String, String>?,
        postData: Hashtable<String, Any>?,
    ) = try {
        client.post {
            url(url)
            headers?.let {
                if (it.isNotEmpty()) headers {
                    for ((key, value) in it) append(key, value)
                }
            }
            postData?.let {
                setBody(TextContent(it.toJsonString(), ContentType.Application.Json))
            }
        }.bodyAsText()
    } catch (e: SocketTimeoutException) {
        logger.error("postRequest() url=$url header=$headers SocketTimeoutException ${e.message}")
        throw ProxyException(e)
    } catch (e: SocketException) {
        logger.error("postRequest() url=$url header=$headers SocketException ${e.message}")
        throw ProxyException(e)
    } catch (e: SSLHandshakeException) {
        logger.error("postRequest() url=$url header=$headers SSLPeerUnverifiedException ${e.message}")
        throw ProxyException(e)
    } catch (e: SSLPeerUnverifiedException) {
        logger.error("postRequest() url=$url header=$headers SSLPeerUnverifiedException ${e.message}")
        throw ProxyException(e)
    } catch (e: Exception) {
        logger.error("postRequest() url=$url header=$headers & postRequest=$postData Error:", e)
        throw e
    }

    override suspend fun postRawResponse(
        url: String,
        headers: Hashtable<String, String>?,
        postData: Hashtable<String, Any>?,
        followRedirect: Boolean,
    ): HttpResponse? = try {
        var cache = true
        client.config {
            cache = this.followRedirects
            this.followRedirects = followRedirect
        }
        val response = client.post {
            url(url)
            headers?.let {
                if (it.isNotEmpty()) {
                    headers {
                        for ((key, value) in it) append(key, value)
                    }
                }
            }
            postData?.let {
                setBody(TextContent(it.toJsonString(), ContentType.Application.Json))
            }
        }
        client.config {
            this.followRedirects = cache
        }
        response
    } catch (e: SSLPeerUnverifiedException) {
        logger.error("getRawResponse() url=$url header=$headers SSLPeerUnverifiedException ${e.message}")
        throw ProxyException(e)
    } catch (e: SocketTimeoutException) {
        logger.error("getRawResponse() url=$url header=$headers SocketTimeoutException ${e.message}")
        throw ProxyException(e)
    } catch (e: SocketException) {
        logger.error("getRawResponse() url=$url header=$headers SocketException ${e.message}")
        throw ProxyException(e)
    } catch (e: SSLHandshakeException) {
        logger.error("getRawResponse() url=$url header=$headers SLLHandShakeException ${e.message}")
        throw ProxyException(e)
    } catch (e: Exception) {
        if (e is TimeoutCancellationException || e is CancellationException) logger.error("getRawResponse() url=$url header=$headers Cancellation exception: ${e.message}")
        else logger.error("getRawResponse() url=$url header=$headers Generic exception:", e)
        null
    }

    // Instagram Server crashes with 500 if we sent wrong cookies
    // So it is tackled by hardcoding and making it as true to prevent NonFatal Error
    override suspend fun checkPageAvailability(
        url: String,
        headers: Hashtable<String, String>?,
    ): Boolean {
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
            var cacheRedirect = true
            client.config {
                cacheRedirect = followRedirects
                followRedirects = false
            }
            client.get {
                url(url)
                method = HttpMethod.Head
                headers?.let {
                    if (it.isNotEmpty()) headers {
                        for ((key, value) in it) append(key, value)
                    }
                }
            }.run {
                client.config { followRedirects = cacheRedirect }
                status in acceptedStatusCode || run {
                    if (status in redirectionStatusCode) {
                        val res = getLastPossibleRedirectedResponse(this, headers)
                        val isPageAvailable = res.status in acceptedStatusCode || res.status in redirectionStatusCode
                        logger.info("page availability = $isPageAvailable")
                        return isPageAvailable
                    }
                    logger.warn("Unhandled in checkWebPage() status code=$status for url=$url with headers=$headers & response=${bodyAsText()}")
                    false
                }
            }
        } catch (e: ClientRequestException) {
            logger.error("checkWebPage() url=$url header=$headers ClientRequestException:", e)
            false
        } catch (e: SocketTimeoutException) {
            logger.error("checkWebPage() url=$url header=$headers SocketTimeoutException ${e.message}")
            throw ProxyException(e)
        } catch (e: SocketException) {
            logger.error("checkWebPage() url=$url header=$headers SocketException ${e.message}")
            throw ProxyException(e)
        } catch (e: SSLHandshakeException) {
            logger.error("checkWebPage() url=$url header=$headers SSLPeerUnverifiedException ${e.message}")
            throw ProxyException(e)
        } catch (e: SSLPeerUnverifiedException) {
            logger.error("checkWebPage() url=$url header=$headers SSLPeerUnverifiedException ${e.message}")
            throw ProxyException(e)
        } catch (e: Exception) {
            logger.error("checkWebPage() url=$url header=$headers GenericException:", e)
            false
        }
    }

    private suspend fun getLastPossibleRedirectedResponse(
        response: HttpResponse,
        headers: Hashtable<String, String>?,
    ): HttpResponse {
        var cacheFollowRedirection = true
        val nonRedirectingClient = client.config {
            cacheFollowRedirection = followRedirects
            followRedirects = false
        }
        var cnt = 0
        var cacheResponse = response
        do {
            var locationUrl = cacheResponse.headers[HttpHeaders.Location] ?: return cacheResponse

            val matcher = Pattern.compile("^(?:https?://)?(?:[^@\\n]+@)?(?:www\\.)?([^:/\\n?]+)").matcher(locationUrl)
            if (!matcher.find()) locationUrl = cacheResponse.request.url.protocolWithAuthority + locationUrl
            logger.info("redirection ${cacheResponse.request.url}->$locationUrl [${cacheResponse.status.value}]")
            val tempResponse = nonRedirectingClient.get(locationUrl) {
                this.headers {
                    headers?.let {
                        for ((key, value) in it) append(key, value)
                    }
                }
            }
            nonRedirectingClient.close()
            if (cacheResponse.request.url == tempResponse.request.url) break
            cacheResponse = tempResponse
            cnt++
        } while (cacheResponse.status in redirectionStatusCode && cnt < 20)
        client.config { followRedirects = cacheFollowRedirection }
        return if (cacheResponse.request.url.host == "localhost") response else cacheResponse
    }

    override fun close() {
        client.close()
    }
}
