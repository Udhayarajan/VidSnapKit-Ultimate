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

import com.mugames.vidsnapkit.dataholders.AudioResource
import com.mugames.vidsnapkit.dataholders.Formats
import com.mugames.vidsnapkit.dataholders.ImageResource
import com.mugames.vidsnapkit.dataholders.VideoResource
import com.mugames.vidsnapkit.network.HttpRequest
import com.mugames.vidsnapkit.toJSONObject
import org.json.JSONObject
import java.util.regex.Pattern


/**
 * @author Udhaya
 * Created on 14-01-2023
 */

class Vimeo internal constructor(url: String) : Extractor(url) {

    private val formats: Formats = Formats()

    companion object {
        const val CONFIG_URL = "https://player.vimeo.com/video/%s/config"
    }

    private fun getVideoId() = Pattern.compile("(?:https|http):\\/\\/(?:www\\.|.*?)vimeo\\.com\\/((?=.*?#).*(?=#)|.*)")
        .matcher(inputUrl)
        .run {
            if (find()) group(1) else null
        }

    override suspend fun analyze() {
        formats.src = "Vimeo"
        formats.url = inputUrl
        val id = getVideoId()
        id?.let {
            val request = HttpRequest(CONFIG_URL.format(it))
            parseConfigRequest(request.getResponse())
            videoFormats.add(formats)
            finalize()
        }
    }

    private suspend fun parseConfigRequest(response: String) {
        val json = response.toJSONObject()
        val hls = json.getJSONObject("request").getJSONObject("files").getJSONObject("dash")
        val defaultCdn = hls.getString("default_cdn")
        val cdnUrl = hls.getJSONObject("cdns").getJSONObject(defaultCdn).getString("url")
        extractFromCdns(HttpRequest(cdnUrl).getResponse(), cdnUrl)
        val video = json.getJSONObject("video")
        extractMetaData(video)
    }

    private fun extractMetaData(video: JSONObject) {
        val thumbs = video.getJSONObject("thumbs")
        thumbs.keys().forEach {
            formats.imageData.add(
                ImageResource(
                    thumbs.getString(it),
                    resolution = it
                )
            )
        }

        formats.title = video.getString("title")
    }

    private fun extractFromCdns(response: String, cdnUrl: String) {
        var url = cdnUrl
        var baseUrl = response.toJSONObject().getString("base_url")
        val videoArray = response.toJSONObject().getJSONArray("video")
        val audioArray = response.toJSONObject().getJSONArray("audio")

        fun getUrlAndMimeFromObject(jsonObject: JSONObject) =
            listOf(
                url + jsonObject.get("base_url") + jsonObject.get("id") + ".mp4",
                jsonObject.getString("mime_type")
            )

        fun extractVideoData() {
            videoArray.forEach {
                if (it !is JSONObject)
                    return@forEach
                val (videoUrl, mime) = getUrlAndMimeFromObject(it)
                formats.videoData.add(
                    VideoResource(
                        videoUrl,
                        mime,
                        "${it.get("width")}x${it.get("height")}",
                        hasAudio = false
                    )
                )
            }
        }

        fun extractAudioData() {
            audioArray.forEach {
                if (it !is JSONObject)
                    return@forEach
                val (audioUrl, mime) = getUrlAndMimeFromObject(it)
                formats.audioData.add(
                    AudioResource(
                        audioUrl,
                        mime
                    )
                )
            }
        }

        val backCount = baseUrl.split("../").dropLastWhile { it.isEmpty() }.toList().size
        baseUrl = baseUrl.replace("../", "")
        repeat(backCount) {
            url = url.replace("(?:.(?!\\/))+\$".toRegex(), "")
        }
        url += "/$baseUrl"
        extractVideoData()
        extractAudioData()
    }
}