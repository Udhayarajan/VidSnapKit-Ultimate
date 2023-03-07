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
import com.mugames.vidsnapkit.dataholders.Formats
import com.mugames.vidsnapkit.dataholders.ImageResource
import com.mugames.vidsnapkit.dataholders.VideoResource
import com.mugames.vidsnapkit.network.HttpRequest
import com.mugames.vidsnapkit.toJSONObject
import java.util.regex.Pattern


/**
 * @author Udhaya
 * Created on 06-03-2023
 */

class Twitch internal constructor(url: String) : Extractor(url) {

    private val localFormats = Formats()
    override suspend fun analyze() {
        TODO("Not yet implemented")
    }

    private fun getId(s: String?): String? {
        return s?.run {
            val matcher =
                Pattern.compile("https?://(?:clips\\.twitch\\.tv/(?:embed\\?.*?\\bclip=|(?:[^/]+/)*)|(?:(?:www|go|m)\\.)?twitch\\.tv/[^/]+/clip/)([^/?#&]+)")
                    .matcher(this)
            if (matcher.find())
                matcher.group(1)
            else null
        }
    }

    suspend fun extractURLFromClips(): Formats? {
        var id: String?
        if (getId(inputUrl).also { id = it } == null) {
            clientRequestError("Error!! Id can't find for URL $inputUrl")
            return null
        }
        headers["Content-Type"] = "text/plain;charset=UTF-8"
        headers["Client-ID"] = "kimne78kx3ncx6brgo4mv6wki5h1ko"
        val data = String.format(
            "{\"query\":\"{clip(slug:\\\"%s\\\"){broadcaster{displayName}createdAt curator{displayName id}durationSeconds id tiny:thumbnailURL(width:86,height:45)small:thumbnailURL(width:260,height:147)medium:thumbnailURL(width:480,height:272)title videoQualities{frameRate quality sourceURL}viewCount}}\"}",
            id
        )
        val response = HttpRequest("https://gql.twitch.tv/gql", headers).postRequest()
        val clip = response.toJSONObject().getJSONObject("data").getJSONObject("clip")
        localFormats.imageData.add(ImageResource(clip.getString("medium"), "medium"))
        val videoQualities = clip.getJSONArray("videoQualities")
        localFormats.title = clip.getString("title")
        for (j in 0 until videoQualities.length()) {
            val video = videoQualities.getJSONObject(j)
            localFormats.videoData.add(
                VideoResource(
                    video.getString("sourceURL"),
                    MimeType.VIDEO_MP4,
                    video.getString("quality") + "p"
                )
            )
        }
        return localFormats
    }
}
