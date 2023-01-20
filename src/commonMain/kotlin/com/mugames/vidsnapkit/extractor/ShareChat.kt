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
import com.mugames.vidsnapkit.getNullableString
import com.mugames.vidsnapkit.network.HttpRequest
import com.mugames.vidsnapkit.dataholders.*
import org.json.JSONObject
import java.util.regex.Pattern

/**
 * @author Udhaya
 * Created on 01-09-2022
 */

class ShareChat internal constructor(url: String) : Extractor(url) {
    private val formats = Formats()

    override suspend fun analyze() {
        formats.url = inputUrl
        formats.src = "ShareChat"
        onProgress(Result.Progress(ProgressState.Start))
        scratchWebPage(HttpRequest(inputUrl).getResponse())
    }

    private suspend fun scratchWebPage(response: String) {
        val matcher =
            Pattern.compile("""<script data-rh="true" type="application\/ld\+json">(\{"@context":"http:\/\/schema\.org","@type":"(?:Image|Video)Object".*?\})<\/script>""")
                .matcher(response)
        if (!matcher.find()) {
            onProgress(Result.Failed(Error.InternalError("Unable detect the contentUrl for $inputUrl")))
        }
        onProgress(Result.Progress(ProgressState.Middle))
        val responseObject = JSONObject(matcher.group(1)!!)
        formats.title = responseObject.getString("name")
            ?: responseObject.getString("description")
                    ?: "ShareChat_${
                responseObject.getJSONObject("author")
                    .getNullableString("name")
            }"
        val contentUrl = responseObject.getString("contentUrl")
        try {
            // Try for video
            val resolution = responseObject.getString("width") + "x" + responseObject.getString("height")
            formats.videoData.add(
                VideoResource(
                    contentUrl,
                    MimeType.VIDEO_MP4,
                    resolution
                )
            )
            formats.imageData.add(ImageResource(responseObject.getString("thumbnail")))
        }catch (e:Exception){
            formats.imageData.add(ImageResource(contentUrl))
        }
        videoFormats.add(formats)
        finalize()
    }
}