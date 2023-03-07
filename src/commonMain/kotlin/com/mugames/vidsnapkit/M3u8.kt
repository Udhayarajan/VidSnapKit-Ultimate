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

package com.mugames.vidsnapkit

import com.mugames.vidsnapkit.dataholders.Error
import com.mugames.vidsnapkit.dataholders.Formats
import com.mugames.vidsnapkit.dataholders.Result
import com.mugames.vidsnapkit.network.HttpRequest
import org.json.JSONObject
import java.util.regex.Pattern


/**
 * @author Udhaya
 * Created on 07-03-2023
 */

class M3u8(private val url: String, private val onProgressCallback: ProgressCallback) {

    var TAG = Statics.TAG + ":m3u8"


    var info: JSONObject? = null


    private val localFormats = Formats()

    private fun nonFatalError(msg: String) {
        onProgressCallback.onProgress(Result.Failed(Error.NonFatalError(msg)))
    }

    fun extractM3u8(url: String, info: JSONObject) {
        if (info["isLive"].equals("true")) {
            nonFatalError("Live video can't be downloaded")
        }
        this.info = info

        // TODO Fix needed
//        realExtract(0, url, object : ChunkCallback {
//            fun onChunkExtracted(index: Int, chunkURLS: ArrayList<String?>?) {
//                //Called only if it is not playlist else completingProcess() directly called from extractFromPlaylist
//                extractor.formats.manifest.add(chunkURLS)
//                extractor.formats.fileMime.add(MIMEType.VIDEO_MP4)
//                completingProcess()
//            }
//        })
    }


    private suspend fun realExtract(index: Int, url: String, chunkCallback: ChunkCallback) {
        val response = HttpRequest(url).getResponse()
        response?.let {
            if (it.contains("#EXT-X-FAXS-CM:")) {
                nonFatalError("Adobe Flash access can't downloaded")
                return
            }
            if (Pattern.compile("#EXT-X-SESSION-KEY:.*?URI=\"skd://").matcher(response).find()) {
                nonFatalError("Apple Fair Play protected can't downloaded")
                return
            }
            if (it.contains("#EXT-X-TARGETDURATION")) {
                //No playlist
                extractFromMeta(index, it, url, chunkCallback)
            } else {
                extractFromPlaylist(it, url)
            }
        } ?: run {
            nonFatalError("Unable to fetch from $url")
        }
    }

    //size
    var got = 0

    //TODO Fix needed
//    fun completingProcess() {
//        try {
//            extractor.formats.title = info.getString("title")
//        } catch (e: JSONException) {
//            extractor.formats.title = "Stream_video"
//        }
//        if (extractor.formats.manifest.size() === 1) {
//            try {
//                extractor.formats.qualities.add(info.getString("resolution"))
//            } catch (e: JSONException) {
//                extractor.formats.qualities.add("--")
//            }
//        }
//        try {
//            extractor.formats.thumbNailsURL.add(info.getString("thumbNailURL"))
//        } catch (e: JSONException) {
//            e.printStackTrace()
//        }
//        extractor.fetchDataFromURLs()
//    }
//

    private fun extractFromPlaylist(response: String, url: String?) {
        val fragUrls: ArrayList<String> = ArrayList()
        // TODO Fix needed
//        for (line in response.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
//            val newLine = line.trim { it <= ' ' }
//            if (!newLine.startsWith("#")) {
//                fragUrls.add(joinURL(url, line))
//                extractor.formats.chunkUrlList.add(fragUrls[fragUrls.size() - 1])
//            } else if (line.contains("RESOLUTION")) {
//                for (l in line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
//                    l = l.trim { it <= ' ' }
//                    if (l.contains("RESOLUTION")) {
//                        extractor.formats.qualities.add(getResolution(l))
//                    }
//                }
//            }
//        }
//        for (i in 0 until fragUrls.size()) {
//            localFormats.manifest.add(null)
//            realExtract(i, fragUrls[i], object : ChunkCallback {
//                fun onChunkExtracted(index: Int, chunkURLS: ArrayList<String?>?) {
//                    extractor.formats.manifest.set(index, chunkURLS)
//                    extractor.formats.fileMime.add(MIMEType.VIDEO_MP4)
//                    got++
//                    if (got == fragUrls.size()) {
//                        got = 0
//                        completingProcess()
//                    }
//                }
//            })
//        }
    }


    private fun extractFromMeta(index: Int, meta: String, url: String, chunkCallback: ChunkCallback) {
        val chunksUrl: ArrayList<String> = ArrayList()
        var mediaFrag = 0
        var adFrag = 0
        var adFragNext = false
        if (canDownload(meta)) {
            for (line in meta.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                val newLine = line.trim { it <= ' ' }
                if (newLine.isEmpty()) continue
                if (newLine.startsWith("#")) {
                    if (is_ad_fragment_start(newLine)) adFragNext =
                        true else if (is_ad_fragment_end(newLine)) adFragNext = false
                    continue
                }
                if (adFragNext) {
                    adFrag++
                    continue
                }
                mediaFrag += 1
            }
            adFragNext = false
            for (line in meta.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
                val newLine = line.trim { it <= ' ' }
                if (newLine.isNotEmpty()) {
                    if (!line.startsWith("#")) {
                        if (adFragNext) continue
                        chunksUrl.add(find_url(url, line))
                    } else if (is_ad_fragment_start(line)) adFragNext = true
                    else if (is_ad_fragment_end(line)) adFragNext = false
                }
            }
        }
        chunkCallback.onChunkExtracted(index, chunksUrl)
    }

    private fun canDownload(meta: String): Boolean {
        for (s in arrayOf("#EXT-X-KEY:METHOD=(?!NONE|AES-128)", "#EXT-X-MAP:")) {
            val pattern: Pattern = Pattern.compile(s)
            if (pattern.matcher(meta).find()) {
                return false
            }
        }
        return if (meta.contains("#EXT-X-KEY:METHOD=AES-128")) false else !meta.contains("#EXT-X-BYTERANGE")
    }

    fun is_ad_fragment_start(metaLine: String): Boolean {
        return metaLine.startsWith("#ANVATO-SEGMENT-INFO") && metaLine.contains("type=ad") || metaLine.startsWith("#UPLYNK-SEGMENT") && metaLine.endsWith(
            ",ad"
        )
    }

    fun is_ad_fragment_end(metaLine: String): Boolean {
        return metaLine.startsWith("#ANVATO-SEGMENT-INFO") && metaLine.contains("type=master") || metaLine.startsWith("#UPLYNK-SEGMENT") && metaLine.endsWith(
            ",segment"
        )
    }

    fun nullOrEmpty(s: String?): Boolean {
        return s == null || s.isEmpty()
    }


    fun find_url(mainURL: String?, line: String): String {
        return if (Pattern.compile("^https?://").matcher(line).find()) line else Util.joinURL(mainURL, line)!!
    }


    fun getResolution(query: String): String? {
        return query.substring("RESOLUTION".length + 1)
    }


    internal interface ChunkCallback {
        fun onChunkExtracted(index: Int, chunkURLS: ArrayList<String>?)
    }

}
