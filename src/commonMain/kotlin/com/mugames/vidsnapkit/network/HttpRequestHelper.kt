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
import java.util.*

/**
 * @author Udhaya
 * Created on 21-01-2022
 */
interface HttpInterface {
    suspend fun getData(url: String, headers: Hashtable<String, String>? = null): String?
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
    override suspend fun postData(
        url: String,
        postData: Hashtable<String, Any>?,
        headers: Hashtable<String, String>?
    ): String {
        return try {
            client.post {
                url(url)
                postData?.let {
                    setBody(TextContent(it.toJsonString(), ContentType.Application.Json))
                }
            }.bodyAsText()
        } catch (e: Error) {
            throw e
        }
    }

    // Instagram Server crashes with 500 if we sent wrong cookies
    // So it is tackled by hardcoding and making it as true to prevent NonFatal Error
    override suspend fun checkWebPage(url: String, headers: Hashtable<String, String>?): Boolean {
        return try {
            client.get {
                url(url)
                headers?.let {
                    if (it.isNotEmpty())
                        headers {
                            for ((key, value) in it)
                                append(key, value)
                        }
                }
            }.run {
                status in setOf(
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
            }
        } catch (e: ClientRequestException) {
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
                else if (url.contains("instagram") && status == HttpStatusCode.InternalServerError) "{error:\"Invalid Cookies\"}"
                else null
            }
        } catch (e: ClientRequestException) {
            null
        } catch (e: Exception) {
            throw e
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

}
