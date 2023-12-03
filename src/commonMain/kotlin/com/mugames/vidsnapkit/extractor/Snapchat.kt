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
import com.mugames.vidsnapkit.toJSONObject
import org.json.JSONObject
import java.util.regex.Pattern


/**
 * @author Udhaya
 * @Created 03-12-2023
 */

class Snapchat internal constructor(url: String) : Extractor(url) {
    override suspend fun analyze(payload: Any?) {
        scratchWebpage(
            httpRequestService.getResponse(inputUrl) ?: run {
                clientRequestError()
                return
            }
        )
    }

    private suspend fun scratchWebpage(webpage: String) {
        val matcher = Pattern.compile("<script id=\"__NEXT_DATA__\" type=\"application/json\">(\\{.*\\})</script>")
            .matcher(webpage)

        if (!matcher.find()) {
            internalError("Unable to detect the contentUrl for $inputUrl")
            return
        }

        val responseJSON = matcher.group(1).toJSONObject()
        val page = responseJSON.getString("page")
        if (page.contains("/spotlight/")) {
            extractSpotlight(responseJSON.getJSONObject("props").getJSONObject("pageProps"))
        } else if (page.contains("/p/")) {
            extractStories(responseJSON.getJSONObject("props").getJSONObject("pageProps"))
        } else {
            missingLogic()
        }
    }

    private suspend fun extractStories(pageProps: JSONObject) {
        missingLogic()
    }

    private suspend fun extractSpotlight(pageProps: JSONObject) {
        val videoMetadata = pageProps.getJSONObject("videoMetadata")
        val format = Formats()
        format.title = videoMetadata.getString("description")
        format.videoData.add(
            VideoResource(
                videoMetadata.getString("contentUrl"),
                MimeType.VIDEO_MP4,
                "${videoMetadata.get("width")}x${videoMetadata.get("height")}",
            )
        )

        format.imageData.add(ImageResource(videoMetadata.getString("thumbnailUrl")))

        videoFormats.add(format)
        finalize()
    }

    override suspend fun testWebpage(string: String) {
        TODO("Not yet implemented")
    }
}
