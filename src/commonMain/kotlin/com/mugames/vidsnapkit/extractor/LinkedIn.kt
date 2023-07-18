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

import com.mugames.vidsnapkit.Util
import com.mugames.vidsnapkit.dataholders.*
import com.mugames.vidsnapkit.network.HttpRequest
import com.mugames.vidsnapkit.toJSONObject
import org.json.JSONArray
import java.util.regex.Pattern

/**
 * @author Udhaya
 * Created on 24-05-2022
 */

class LinkedIn internal constructor(url: String) : Extractor(url) {
    private val formats = Formats()

    override suspend fun analyze(payload: Any?) {
        formats.url = inputUrl
        formats.src = "LinkedIn"
        onProgress(Result.Progress(ProgressState.Start))
        scratchWebpage(
            HttpRequest(inputUrl, headers).getResponse() ?: run {
                clientRequestError()
                return
            }
        )
    }

    override suspend fun testWebpage(string: String) {
        TODO("Not yet implemented")
    }

    private suspend fun scratchWebpage(page: String) {
        val matcher = Pattern.compile("data-sources=\"(.*?)\"").matcher(page)
        if (matcher.find()) {
            val jsonString = Util.decodeHTML(matcher.group(1))
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val data = jsonArray.getJSONObject(i)
                formats.videoData.add(
                    VideoResource(
                        data.getString("src"),
                        data.getString("type"),
                        getResolutionFromVideoUrl(data.getString("src"))
                    )
                )
            }
            formats.title = getTitle(page)
            getThumbnailData(page)
            videoFormats.add(formats)
            finalize()
            return
        }
        brutForceWithQuotData(page)
    }

    private fun getTitle(page: String): String {
        fun findTitle(vararg regexes: Regex, defaultTitle: String): String {
            for (regex in regexes) {
                val m = Pattern.compile(regex.toString()).matcher(page)
                if (m.find()) {
                    return m.group(1) ?: defaultTitle
                }
            }
            return defaultTitle
        }
        return findTitle(
            Regex("\"twitter:title\"\\s*content\\s*=\\s*\"(.*?)\">"),
            Regex("\"og:title\"\\s*content\\s*=\\s*\"(.*?)\">"),
            Regex("\"twitter:description\"\\s*content\\s*=\\s*\"([\\w\\W]*?)\">"),
            Regex("\"og:description\"\\s*content\\s*=\\s*\"([\\w\\W]*?)\">"),
            defaultTitle = "LinkedIn_Video"
        )
    }

    private suspend fun brutForceWithQuotData(page: String) {
        val matcher =
            Pattern.compile("<code style=\"display: none\" id=\".*?\">\\W*?(\\{(?:&quot;|\")data.*\\})\\W*</")
                .matcher(page)
        while (matcher.find()) {
            val match = matcher.group(1)
            if (match.contains("adaptiveStreams") || match.contains("progressiveStreams")) {
                formats.title = getTitle(page)
                val included = Util.decodeHTML(match)?.toJSONObject()?.getJSONArray("included")
                if (included == null || included.length() == 0)
                    continue
                extractFromIncluded(included)
                return
            }
        }
        if (page.contains("Please enter your email address", true)) {
            onProgress(Result.Failed(Error.LoginRequired))
            return
        }
        onProgress(Result.Failed(Error.MethodMissingLogic))
    }

    private suspend fun extractFromIncluded(included: JSONArray) {
        // https://www.linkedin.com/feed/update/urn:li:activity:7072066734848946176?utm_source=share&utm_medium=member_android
        // TODO: Still HLS extraction not supported in VidSnap. When future support is added make sure this area also has HLS
        // Above link has HLS format too
        for (i in 0 until included.length()) {
            val obj = included.getJSONObject(i)
            if (!obj.has("adaptiveStreams") || !obj.has("progressiveStreams"))
                continue
            val rootUrl = obj.getJSONObject("thumbnail").getString("rootUrl")
            val images = obj.getJSONObject("thumbnail").getJSONArray("artifacts")
            for (j in 0 until images.length()) {
                val image = images.getJSONObject(j)
                formats.imageData.add(
                    ImageResource(
                        (rootUrl + image.getString("fileIdentifyingUrlPathSegment")).replace(" ", ""),
                        resolution = "${image.getInt("width")}x${image.getInt("height")}"
                    )
                )
            }
            val videos = obj.getJSONArray("progressiveStreams")
            for (j in 0 until videos.length()) {
                val video = videos.getJSONObject(i)
                formats.videoData.add(
                    VideoResource(
                        video.getJSONArray("streamingLocations").getJSONObject(0).getString("url").replace(" ", ""),
                        video.getString("mediaType"),
                        "${video.getInt("width")}x${video.getInt("height")}"
                    )
                )
            }
            videoFormats.add(formats)
            finalize()
            return
        }
    }

    private fun getThumbnailData(page: String) {
        fun findThumbnail(vararg regexes: Regex): String? {
            for (regex in regexes) {
                val matcher = Pattern.compile(regex.toString()).matcher(page)
                if (matcher.find()) return Util.decodeHTML(matcher.group(1))
            }
            return null
        }

        val thumbnailUrl = findThumbnail(
            Regex("\"twitter:image\"\\s*content\\s*=\\s*\"(.*?)\">"),
            Regex("\"og:image\"\\s*content\\s*=\\s*\"(.*?)\">")
        )
        thumbnailUrl?.let {
            formats.imageData.add(ImageResource(it))
        }
    }

    private fun getResolutionFromVideoUrl(url: String): String {
        val matcher = Pattern.compile("mp4-(.*?)-").matcher(url)
        if (matcher.find()) {
            return matcher.group(1) ?: "--"
        }
        return "--"
    }
}
