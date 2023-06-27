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

import com.mugames.vidsnapkit.*
import com.mugames.vidsnapkit.dataholders.*
import com.mugames.vidsnapkit.network.HttpRequest
import java.util.regex.Pattern

/**
 * @author Udhaya
 * Created on 25-12-2022
 */

class DailyMotion(url: String) : Extractor(url) {
    lateinit var localFormats: Formats

    companion object {
        const val METADATA_API = "https://www.dailymotion.com/player/metadata/video/%s"
    }

    private fun getVideoId(): String? {
        val matcher =
            Pattern.compile("(?:(?:https?:\\/\\/|.*)(?:[w]+\\.|.*))?(?:dailymotion\\.com|dai\\.ly)\\/?(?:video\\/|)((?=.*?[\\?\\/]).*?(?=[\\?\\/])|.*)")
                .matcher(inputUrl)
        if (matcher.find()) return matcher.group(1)
        return null
    }

    override suspend fun analyze(payload: Any?) {
        localFormats = Formats()
        val id = getVideoId()
        id?.let {
            onProgress(Result.Progress(ProgressState.Start))
            val json = HttpRequest(METADATA_API.format(id)).getResponse()?.toJSONObject() ?: run {
                clientRequestError()
                return
            }
            json.getNullableJSONObject("error")?.let {
                clientRequestError(it.getString("raw_message"))
                return
            }

            localFormats.title = json.getString("title")
            localFormats.url = inputUrl
            localFormats.src = "DailyMotion"

            val thumbnails = json.getJSONObject("posters")

            for (i in listOf(
                "60",
                "120",
                "180",
                "240",
                "360",
                "480",
                "720",
                "1080"
            )) {
                localFormats.imageData.add(
                    ImageResource(
                        thumbnails.getString(i),
                        resolution = i
                    )
                )
            }

            val qualities = json.getJSONObject("qualities")
            val autoQuality = qualities.getNullableJSONArray("auto")
            for (i in 0 until (autoQuality?.length() ?: 0)) {
                val type = autoQuality!!.getJSONObject(i).getString("type")
                if (type == MimeType.APPLICATION_X_MPEG_URL) {
                    extractFromM3U8(
                        HttpRequest(
                            autoQuality.getJSONObject(i).getString("url")
                        ).getResponse() ?: run {
                            clientRequestError()
                            return
                        }
                    )
                } else {
                    localFormats.videoData.add(
                        VideoResource(
                            autoQuality.getJSONObject(i).getString("url"),
                            type
                        )
                    )
                }
            }
        } ?: run {
            onProgress(Result.Failed(Error.InvalidUrl))
        }
    }

    override suspend fun testWebpage(string: String) {
        TODO("Not yet implemented")
    }

    private suspend fun extractFromM3U8(response: String) {
        fun valueForKey(key: String, line: String): String? {
            val matcher = Pattern.compile("$key=(?:\"(.*?)\"|(.*?),)").matcher(line)
            return if (matcher.find())
                matcher.tryGroup(1) ?: matcher.tryGroup(2) else null
        }

        onProgress(Result.Progress(ProgressState.Middle))
        for (line in response.split("\n")) {
            if (line.contains("#EXT-X-STREAM-INF")) {
                valueForKey("PROGRESSIVE-URI", line)?.let {
                    localFormats.videoData.add(
                        VideoResource(
                            it,
                            MimeType.VIDEO_MP4,
                            valueForKey("RESOLUTION", line) ?: "--"
                        )
                    )
                }
            }
        }
        videoFormats.add(localFormats)
        finalize()
    }
}
