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

package com.mugames.vidsnapkit.extractor

import com.mugames.vidsnapkit.ProgressCallback
import com.mugames.vidsnapkit.dataholders.Error
import com.mugames.vidsnapkit.dataholders.Formats
import com.mugames.vidsnapkit.dataholders.ProgressState
import com.mugames.vidsnapkit.dataholders.Result
import com.mugames.vidsnapkit.network.HttpRequest
import com.mugames.vidsnapkit.sanitizeAsHeaderValue
import io.ktor.client.plugins.*
import kotlinx.coroutines.*
import kotlinx.coroutines.future.future
import java.util.*

/**
 * @author Udhaya
 * Created on 21-01-2022
 *
 *  This is super class of all kind of Extractor
 *
 *  Don't directly create instance of [Extractor]
 *  abstract class
 *
 *  use [Extractor.findExtractor] to get required extractor
 */
abstract class Extractor(
    url: String,
) {

    companion object {
        /**
         * @param url direct URL from user input
         * @return Child class of [Extractor] based on input URL
         * and `null` if no suitable [Extractor] found
         */
        fun findExtractor(
            url: String,
        ): Extractor? {
            return when {
                url.contains("facebook|fb".toRegex()) -> Facebook(url)
                url.contains("instagram") -> Instagram(url)
                url.contains("linkedin") -> LinkedIn(url)
                url.contains("sharechat") -> ShareChat(url)
                url.contains("dailymotion|dai.ly".toRegex()) -> DailyMotion(url)
                url.contains("vimeo") -> Vimeo(url)
                url.contains("likee") -> Likee(url)
                else -> null
            }
        }
    }

    protected var inputUrl: String = url
    protected lateinit var onProgress: (Result) -> Unit


    protected var headers: Hashtable<String, String> = Hashtable()

    /**
     * If media is private just pass valid cookies to
     * extract list of [Formats]
     */
    var cookies: String? = null
        set(value) {
            value?.let {
                headers["Cookie"] = it.sanitizeAsHeaderValue()
            }
            field = value
        }


    protected val videoFormats = mutableListOf<Formats>()


    /**
     * starting point of all child of [Extractor]
     * where net safe analyze will be called
     */
    suspend fun start(progressCallback: ProgressCallback) {
        onProgress = {
            progressCallback.onProgress(it)
        }
        safeAnalyze()
    }

    fun startAsync(progressCallback: ProgressCallback) {
        GlobalScope.future { start(progressCallback) }
    }

    suspend fun start(progressCallback: (Result) -> Unit) {
        onProgress = progressCallback
        safeAnalyze()
    }


    private suspend fun safeAnalyze() {
        try {
            analyze()
        } catch (e: Exception) {
            if (e is ClientRequestException && inputUrl.contains("instagram"))
                onProgress(Result.Failed(Error.Instagram404Error(cookies != null)))
            else
                onProgress(Result.Failed(Error.InternalError("Error in SafeAnalyze", e)))
        }
    }


    protected abstract suspend fun analyze()

    protected suspend fun finalize() {
        onProgress(Result.Progress(ProgressState.End))
        withContext(Dispatchers.IO) {
            for (format in videoFormats) {
                val video = async { getVideoSize(format) }
                val audio = async { getAudioSize(format) }
                val image = async { getImageSize(format) }
                val videoSize = video.await()
                val audioSize = audio.await()
                val imageSize = image.await()

                format.videoData.forEachIndexed { idx, elem ->
                    elem.size = videoSize[idx]
                }
                format.audioData.forEachIndexed { idx, elem ->
                    elem.size = audioSize[idx]
                }
                format.imageData.forEachIndexed { idx, elem ->
                    elem.size = imageSize[idx]
                }
            }
            val filteredFormats = mutableListOf<Formats>()
            for (formats in videoFormats) {
                val format = Formats(formats.title, formats.url, formats.src)
                format.videoData.addAll(formats.videoData.filter { it.size > 0 }.toList())
                format.audioData.addAll(formats.audioData.filter { it.size > 0 }.toList())
                format.imageData.addAll(formats.imageData.filter { it.size > 0 }.toList())
                filteredFormats.add(format)
            }
            onProgress(Result.Success(filteredFormats))
        }
    }

    private suspend fun getVideoSize(format: Formats): List<Long> {
        val sizes = mutableListOf<Deferred<Long>>()
        coroutineScope {
            for (videoData in format.videoData) {
                sizes.add(async { HttpRequest(videoData.url).getSize() })
            }
        }
        return sizes.awaitAll()
    }

    private suspend fun getAudioSize(format: Formats): List<Long> {
        val sizes = mutableListOf<Deferred<Long>>()
        coroutineScope {
            for (audioData in format.audioData) {
                sizes.add(async { HttpRequest(audioData.url).getSize() })
            }
        }
        return sizes.awaitAll()
    }

    private suspend fun getImageSize(format: Formats): List<Long> {
        val sizes = mutableListOf<Deferred<Long>>()
        coroutineScope {
            for (imageData in format.imageData) {
                sizes.add(async { HttpRequest(imageData.url).getSize() })
            }
        }
        return sizes.awaitAll()
    }
}