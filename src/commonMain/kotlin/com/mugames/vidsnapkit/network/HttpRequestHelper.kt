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
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.util.*

/**
 * @author Udhaya
 * Created on 21-01-2022
 */
interface HttpInterface {
    suspend fun getData(url: String, headers: Hashtable<String, String>? = null): String
    suspend fun getSize(url: String, headers: Hashtable<String, String>? = null): Long
}

class HttpInterfaceImpl(
    private val client: HttpClient,
) : HttpInterface {
    override suspend fun getData(url: String, headers: Hashtable<String, String>?): String {
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
            }.body()
        } catch (e: Error) {
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