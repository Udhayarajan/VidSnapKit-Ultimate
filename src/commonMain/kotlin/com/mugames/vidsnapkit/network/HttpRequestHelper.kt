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
        }.headers["content-length"]?.toLong() ?: Long.MIN_VALUE
    }

}