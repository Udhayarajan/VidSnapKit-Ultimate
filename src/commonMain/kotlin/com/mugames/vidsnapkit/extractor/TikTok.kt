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

import com.mugames.vidsnapkit.MimeType
import com.mugames.vidsnapkit.dataholders.*
import com.mugames.vidsnapkit.network.HttpRequest
import com.mugames.vidsnapkit.toJSONObject
import java.util.regex.Pattern


/**
 * @author Udhaya
 * Created on 07-03-2023
 */

class TikTok internal constructor(url: String) : Extractor(url) {

    private val localFormats = Formats()


    override suspend fun analyze(payload: Any?) {
        localFormats.src = "TikTok"
        localFormats.url = inputUrl
        val response = HttpRequest(inputUrl).getResponse()
        response?.let {
            extractFromWebPage(response)
        }
    }

    private suspend fun extractFromWebPage(webpage: String) {
        val matcher =
            Pattern.compile("<script id=\"SIGI_STATE\" type=\"application/json\">(\\{.*?\\})</script>").matcher(webpage)
        if (matcher.find()) {
            val json = matcher.group(1).toJSONObject()
            val itemList = json.getJSONObject("ItemList").getJSONObject("video").getJSONArray("list")
            val itemModule = json.getJSONObject("ItemModule")
            for (i in 0 until itemList.length()) {
                val videoId = itemModule.getJSONObject(itemList.getString(i))
                val formats = localFormats.copy(
                    title = "",
                    audioData = mutableListOf(),
                    videoData = mutableListOf(),
                    imageData = mutableListOf()
                )

                formats.title = videoId.getString("desc")
                val video = videoId.getJSONObject("video")
                formats.imageData.add(
                    ImageResource(
                        video.getString("cover")
                    )
                )
                formats.videoData.add(
                    VideoResource(
                        video.getString("playAddr"),
                        MimeType.fromCodecs(video.getString("format")),
                        quality = video.getString("definition")
                    )
                )
                videoFormats.add(formats)
            }
            finalize()
        } else
            onProgress(Result.Failed(Error.MethodMissingLogic))
    }

    override suspend fun testWebpage(string: String) {
        onProgress = {
            println(it)
        }
        extractFromWebPage(string)
    }
}
