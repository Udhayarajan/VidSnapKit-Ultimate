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
import com.mugames.vidsnapkit.Util.Companion.decodeHTML
import com.mugames.vidsnapkit.dataholders.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.collections.set
import kotlin.system.measureTimeMillis

/**
 * @author Udhaya
 * Created on 21-01-2022
 */
class Facebook internal constructor(url: String) : Extractor(url) {
    private val localFormats = Formats()

    private var userAgent = """
        Mozilla/5.0 (Windows NT 10.0; Win64; x64) 
        AppleWebKit/537.36 (KHTML, like Gecko) 
        Chrome/69.0.3497.122 Safari/537.36
    """.trimIndent().replace("\n", "")

    private var isBucket = false

    private suspend fun isCookieValid(): Boolean {
        if (cookies.isNullOrEmpty()) return false
        val res = httpRequestService.getRawResponse("https://www.facebook.com/", headers, false) ?: return false
        if (res.status == HttpStatusCode.OK) {
            val restrictedKeywords = listOf("Create new account", "log in or sign up", "Forgotten password")
            val containsRestrictedKeyword = restrictedKeywords.any { keyword ->
                res.bodyAsText().contains(keyword, ignoreCase = true)
            }
            logger.info("Check cookie containsRestrictedKeyword=$containsRestrictedKeyword ")
            return !containsRestrictedKeyword
        }
        if (res.status == HttpStatusCode.Found) {
            val loc = res.headers["location"]
            logger.info("Oops! redirection found for $loc")
            if (loc.toString().contains("checkpoint")) {
                return false
            }
        }
        return true
    }

    override suspend fun analyze(payload: Any?) {
        localFormats.url = inputUrl
        localFormats.src = "Facebook"

        fun findVideoId(): String? {
            var pattern =
                Pattern.compile("(?:https?://(?:[\\w-]+\\.)?(?:facebook\\.com|facebookcorewwwi\\.onion)/(?:[^#]*?#!/)?(?:(?:video/video\\.php|photo\\.php|video\\.php|video/embed|story\\.php|watch(?:/live)?/?)\\?(?:.*?)(?:v|video_id|story_fbid)=|[^/]+/videos/(?:[^/]+/)?|[^/]+/posts/|groups/[^/]+/permalink/|watchparty/)|facebook:)([0-9]+)")
            var matcher = pattern.matcher(inputUrl)
            return when {
                matcher.find() -> matcher.group(1)
                inputUrl.contains("fb.") -> {
                    if (!inputUrl.endsWith("/")) inputUrl = inputUrl.plus("/")
                    pattern = Pattern.compile("https?://fb\\.watch/(.*?)/")
                    matcher = pattern.matcher(inputUrl)
                    if (matcher.find()) matcher.group(1) else null
                }

                else -> null
            }
        }
        headers["Accept"] =
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
        headers["Accept-Language"] = "en-GB, en-US, en"
        headers["User-Agent"] = userAgent
        if (inputUrl.startsWith("facebook:")) inputUrl = "https://www.facebook.com/video/video.php?v=${findVideoId()}"

        if (!isCookieValid()) cookies = null

        inputUrl = inputUrl.replace("://m.facebook\\.com/".toRegex(), "://en-gb.facebook.com/")
        inputUrl = inputUrl.replace("://www.facebook\\.com/".toRegex(), "://en-gb.facebook.com/")

        try {
            onProgress(Result.Progress(ProgressState.Start))
            extractInfo()
        } catch (e: JSONException) {
            e.printStackTrace()
            onProgress(Result.Failed(Error.InternalError("Something went wrong", e)))
        } catch (e: Exception) {
            logger.warn("$TAG+ analyze: ", e)
            throw e
        }
    }

    private suspend fun extractInfo() {
        var page = ""
        val delay = measureTimeMillis {
            page = httpRequestService.getResponse(inputUrl, headers) ?: run {
                clientRequestError()
                return
            }
        }
        println(delay)
        scratchWebPage(page)
    }

    private suspend fun scratchWebPage(webPage: String) {
        onProgress(Result.Progress(ProgressState.Middle))
        var serverJsData: String? = null
        var matcher = Pattern.compile("handleServerJS\\((\\{.+\\})(?:\\);|,\")").matcher(webPage)
        if (matcher.find()) serverJsData = matcher.group(1) else {
            matcher = Pattern.compile("\\bs\\.handle\\((\\{.+?\\})\\);").matcher(webPage)
            if (matcher.find()) serverJsData = matcher.group(1)
        }
        var videoData: Any? = null
        if (!serverJsData.isNullOrBlank()) {
            JSONObject(serverJsData).getNullableJSONArray("instances")?.let {
                videoData = grabVideoData(it)
            }
        }
        if (videoData == null) {
            matcher =
                Pattern.compile("bigPipe\\.onPageletArrive\\((\\{.+?\\})\\)\\s*;\\s*\\}\\s*\\)\\s*,\\s*[\"']onPageletArrive\\s+$PAGELET_REGEX")
                    .matcher(webPage)
            if (matcher.find()) serverJsData = matcher.group(1) else {
                matcher = Pattern.compile(
                    String.format(
                        "bigPipe\\.onPageletArrive\\((\\{.*?id\\s*:\\s*\\\"%s\\\".*?\\})\\);", PAGELET_REGEX
                    )
                ).matcher(webPage)
                if (matcher.find()) serverJsData = matcher.group(1)
            }
            if (!serverJsData.isNullOrBlank()) videoData = grabFromJSModsInstance(serverJsData.toJSONObject())
        }
        if (videoData == null) {
            videoData = grabRelayPrefetchedDataSearchUrl(webPage)
        }
        if (videoData == null) {
            matcher = Pattern.compile("class=\"[^\"]*uiInterstitialContent[^\"]*\"><div>(.*?)</div>").matcher(webPage)
            if (matcher.find()) {
                onProgress(
                    Result.Failed(
                        Error.NonFatalError(
                            "This video unavailable. FB says : ${matcher.group(1)}"
                        )
                    )
                )
                return
            }
            if (webPage.contains("You must log in to continue")) {
                onProgress(Result.Failed(Error.LoginRequired))
                return
            }
        }
        videoData?.let {
            var m: Matcher
            fun extractThumbnail(vararg regexes: Regex) {
                for (regex in regexes) {
                    m = Pattern.compile(regex.toString()).matcher(webPage)
                    if (m.find()) {
                        localFormats.imageData.add(
                            ImageResource(
                                resolution = Util.getResolutionFromUrl(
                                    m.group(
                                        1
                                    )!!
                                ),
                                url = decodeHTML(m.group(1)!!)!!
                            )
                        )
                        return
                    }
                }
            }

            fun extractTitle(vararg regexes: Regex, default: String = "") {
                for (regex in regexes) {
                    m = Pattern.compile(regex.toString()).matcher(webPage)
                    if (m.find()) {
                        localFormats.title = decodeHTML(m.group(1)!!).toString()
                        return
                    }
                    localFormats.title = default
                }
            }

            if (localFormats.imageData.isEmpty()) extractThumbnail(
                Regex("\"thumbnailImage\":\\{\"uri\":\"(.*?)\"\\}"),
                Regex("\"thumbnailUrl\":\"(.*?)\""),
                Regex("\"twitter:image\"\\s*?content\\s*?=\\s*?\"(.*?)\"")
            )

            if (localFormats.title.isEmpty() || localFormats.title == "null") {
                extractTitle(
                    Regex("(?:true|false),\"name\":\"(.*?)\",\"savable"),
                    Regex("<[Tt]itle id=\"pageTitle\">(.*?) \\| Facebook<\\/title>"),
                    Regex("title\" content=\"(.*?)\""),

                    default = "Facebook_Video"
                )
            }
            if (videoFormats.isEmpty()) {
                videoFormats.add(localFormats)
            }
            finalize()
        } ?: apply {
            val uuid = "fb." + UUID.randomUUID().toString() + ".html"
            File(uuid).writeText(webPage)
            onProgress(Result.Failed(Error.NonFatalError("Sorry! we can't see the page, refer=$uuid")))
        }
    }

    private fun bruteForceJSON(webPage: String, filter: Array<String>): String? {
        val m =
            Pattern.compile("<script type=\"application/json\" data-content-len=\"\\d+\" data-sjs>(\\{.+\\})</script>")
                .matcher(webPage)
        while (m.find()) {
            val json = m.group(1)
            for (word in filter) {
                if (json?.contains(word) == true) {
                    return json
                }
            }
        }
        return null
    }

    private fun grabRelayPrefetchedDataSearchUrl(webpage: String): Any? {
        fun parseAttachment(attachment: JSONObject?, key: String): Formats? {
            val media = attachment?.getNullableJSONObject(key)
            media?.let {
                if (it.getString("__typename") == "Video") {
                    return parseGraphqlVideo(it)
                } else if (it.getString("__typename") == "Photo") {
                    return parseGraphqlImage(it)
                }
            }
            return null
        }

        val data = grabRelayPrefetchedData(webpage, arrayOf("\"dash_manifest\"", "\"playable_url\""))
        data?.let {
            var nodes = it.getNullableJSONArray("nodes")
            var node = it.getNullableJSONObject("node")

            val bucket = data.getNullableJSONObject("bucket")
            if (bucket != null) {
                nodes = bucket.getJSONObject("unified_stories").getJSONArray("edges")
                isBucket = true
            }
            if (nodes == null && node != null) {
                nodes = JSONArray().apply {
                    put(data)
                }
            }

            nodes?.let { nodesIt ->
                for (i in 0 until nodesIt.length()) {
                    node = nodesIt.getNullableJSONObject(i)?.getNullableJSONObject("node")

                    val story =
                        node!!.getNullableJSONObject("comet_sections")?.getJSONObject("content")?.getJSONObject("story")
                            ?: node!!

                    val attachments = story.getNullableJSONObject("attached_story")?.getNullableJSONArray("attachments")
                        ?: story.getJSONArray("attachments")

                    for (j in 0 until attachments.length()) {
                        // attachments.getJSONObject(j).getJSONObject("style_type_renderer").getJSONObject("attachment");
                        val attachment = attachments.getNullableJSONObject(j)?.run {
                            getNullableJSONObject("style_type_renderer") ?: getNullableJSONObject("styles")
                        }?.getNullableJSONObject("attachment") ?: attachments.getJSONObject(j)

                        val ns = attachment?.getNullableJSONObject("all_subattachments")?.getNullableJSONArray("nodes")

                        ns?.let { nsIt ->
                            for (l in 0 until nsIt.length()) {
                                parseAttachment(nsIt.getJSONObject(l), "media")?.let {
                                    videoFormats.add(it)
                                }
                            }
                        }
                        parseAttachment(attachment, "media")?.let {
                            videoFormats.add(it)
                        }
                    }
                }
            }

            val edges =
                it.getNullableJSONObject("mediaset")?.getNullableJSONObject("currMedia")?.getNullableJSONArray("edges")

            if (edges != null) {
                for (j in 0 until edges.length()) {
                    val edge = edges.getJSONObject(j)
                    parseAttachment(edge, "node")?.let {
                        videoFormats.add(it)
                    }
                }
            }

            val video = it.getNullableJSONObject("video")

            if (video != null) {
                val attachments: JSONArray? = video.getNullableJSONObject("story")?.getNullableJSONArray("attachments")
                    ?: video.getNullableJSONObject("creation_story")?.getNullableJSONArray("attachments")

                if (attachments != null) {
                    for (j in 0 until attachments.length()) {
                        parseAttachment(attachments.getJSONObject(j), "media")?.let {
                            videoFormats.add(it)
                        }
                    }
                } else {
                    videoFormats.add(extractFromCreationStory(video))
                    return SUCCESS
                }
                if (videoFormats.isEmpty()) videoFormats.add(parseGraphqlVideo(video))
            }
            return SUCCESS
        }
        return null
    }

    private fun grabRelayData(webPage: String, searchWords: Array<String>): String? {
        val m = Pattern.compile("handleWithCustomApplyEach\\(.*?,(.*)\\);").matcher(webPage)
        while (m.find()) {
            val m1 = Pattern.compile("(\\{.*[^);]\\})\\);").matcher(Objects.requireNonNull(m.group(1)))
            if (m1.find()) for (s in searchWords) {
                val temp = m1.group(1)!!
                if (temp.contains(s)) return m1.group(1)
            }
        }
        return null
    }

    private fun grabRelayPrefetchedData(webPage: String, filter: Array<String>): JSONObject? {
        val jsonString = grabRelayData(webPage, filter) ?: bruteForceJSON(webPage, filter)

        fun searchFromRequireArray(require: JSONArray?): JSONObject? {
            if (require == null) return null
            for (i in 0 until require.length()) {
                val array = require.getJSONArray(i)
                if (array.getString(0) in listOf(
                        "ScheduledServerJS", "ScheduledServerJSWithCSS"
                    )
                ) return searchFromRequireArray(
                    array.getJSONArray(3).getJSONObject(0).getJSONObject("__bbox").getJSONArray("require")
                )
                if (array.getString(0).contains("RelayPrefetchedStreamCache")) return array.getJSONArray(3)
                    .getJSONObject(1).getJSONObject("__bbox").getJSONObject("result").getJSONObject("data")
                if (array.getString(0).contains("ScheduledServerJSWithServer")) return searchFromRequireArray(
                    array.getJSONObject(0).getJSONObject("__box").getJSONArray("require")
                )
            }
            return null
        }
        return searchFromRequireArray(jsonString?.toJSONObjectOrNull()?.getJSONArray("require"))
    }

    private fun parseGraphqlVideo(media: JSONObject, hasCreationStory: Boolean = true): Formats {
        if (media.getNullableJSONObject("creation_story") != null && hasCreationStory) {
            return extractFromCreationStory(media)
        }
        val scopedFormats = localFormats.copy(
            title = "", videoData = mutableListOf(), audioData = mutableListOf(), imageData = mutableListOf()
        )

        val thumbnailUrl = media.getNullableJSONObject("thumbnailImage")?.getString("uri")
            ?: media.getNullableJSONObject("preferred_thumbnail")?.getJSONObject("image")?.getString("uri") ?: ""
        val thumbnailRes = Util.getResolutionFromUrl(thumbnailUrl)
        if (thumbnailUrl != "") scopedFormats.imageData.add(
            ImageResource(
                resolution = thumbnailRes,
                url = thumbnailUrl
            )
        )
        scopedFormats.title = media.getNullableString("name") ?: media.getNullableJSONObject("savable_description")
            ?.getNullableString("text") ?: media.getNullableJSONObject("title")?.getString("text")?.ifEmpty {
            "Facebook_Video"
        } ?: "Facebook_Video"

        val dashXml = media.getNullableString("dash_manifest")
        dashXml?.let {
            val dashFormats = extractFromDash(it)
            scopedFormats.videoData.addAll(dashFormats.videoData)
            scopedFormats.imageData.addAll(dashFormats.imageData)
        }

        fun getWidth() = media.getNullable("width") ?: media["original_width"].toString()
        fun getHeight() = media.getNullable("height") ?: media["original_height"].toString()

        val res = "${getWidth()}x${getHeight()}"

        for (suffix in arrayOf("", "_quality_hd")) {
            val playableUrl = media.getNullableString("playable_url$suffix")
            if (playableUrl == null || playableUrl == "null") continue
            scopedFormats.videoData.add(
                VideoResource(
                    playableUrl, MimeType.VIDEO_MP4, if (suffix == "") "$res(SD)" else "$res(HD)"
                )
            )
        }
        return scopedFormats
    }

    private fun parseGraphqlImage(media: JSONObject): Formats? {
        val scopedFormats = localFormats.copy(
            title = "", videoData = mutableListOf(), audioData = mutableListOf(), imageData = mutableListOf()
        )

        val image = media.getJSONObject("image")
        scopedFormats.imageData.add(
            ImageResource(
                image.getString("uri"), resolution = "${image.get("width")}x${image.get("height")}"
            )
        )

        val blurredImage = media.getJSONObject("blurred_image")
        scopedFormats.imageData.add(
            ImageResource(
                blurredImage.getString("uri"), resolution = Util.getResolutionFromUrl(blurredImage.getString("uri"))
            )
        )

        val previewImage = media.getJSONObject("previewImage")
        scopedFormats.imageData.add(
            ImageResource(
                previewImage.getString("uri"), resolution = Util.getResolutionFromUrl(previewImage.getString("uri"))
            )
        )
        return scopedFormats
    }

    private fun extractFromCreationStory(media: JSONObject): Formats {
        val playbackVideo =
            media.getNullableJSONObject("creation_story")?.getNullableJSONObject("short_form_video_context")
                ?.getNullableJSONObject("playback_video")
        return if (playbackVideo != null) parseGraphqlVideo(playbackVideo)
        else parseGraphqlVideo(media, false)
    }

    private fun extractFromDash(xml: String): Formats {
        fun getRepresentationArray(adaptionSet: JSONArray, index: Int) = with(adaptionSet.getJSONObject(index)) {
            "Representation".let { key ->
                getNullableJSONArray(key) ?: run {
                    JSONArray().apply {
                        put(getJSONObject(key))
                    }
                }
            }
        }

        val scopedFormats = localFormats.copy(
            title = "", videoData = mutableListOf(), audioData = mutableListOf(), imageData = mutableListOf()
        )

        var xmlDecoded = xml.replace("x3C".toRegex(), "<")
        xmlDecoded = xmlDecoded.replace("\\\\\u003C".toRegex(), "<")
        var videos = JSONArray()
        var audios = JSONArray()
        try {
            XMLParserFactory.createParserFactory().xmlToJsonObject(xmlDecoded).getJSONObject("MPD")
                .getJSONObject("Period").run {
                    "AdaptationSet".let { adaptationSet ->
                        getNullableJSONArray(adaptationSet)?.let {
                            videos = getRepresentationArray(it, 0)
                            audios = getRepresentationArray(it, 1)
                        } ?: run {
                            videos = getJSONObject(adaptationSet).getNullableJSONArray("Representation") ?: run {
                                val arr = JSONArray()
                                arr.put(getJSONObject(adaptationSet).getJSONObject("Representation"))
                            }
                        }
                    }
                }

            fun safeGet(jsonObject: JSONObject, key: String) =
                jsonObject.getNullable("_$key") ?: jsonObject.getNullable(key) ?: "--NA--"

            fun addVideos() {
                for (i in 0 until videos.length()) {
                    val video = videos.getJSONObject(i)
                    val videoUrl = video.getString("BaseURL")
                    val res: String = try {
                        "${safeGet(video, "FBQualityLabel")}(${safeGet(video, "FBQualityClass").uppercase()})"
                    } catch (e: JSONException) {
                        "${safeGet(video, "width")}x${safeGet(video, "height")}"
                    }
                    val videoMime = safeGet(video, "mimeType")
                    scopedFormats.videoData.add(
                        VideoResource(
                            videoUrl, videoMime, res, hasAudio = false
                        )
                    )
                }
            }

            fun addAudios() {
                for (i in 0 until audios.length()) {
                    val audio = audios.getJSONObject(i)
                    val audioUrl = safeGet(audio, "BaseURL")
                    val audioMime = safeGet(audio, "mimeType")
                    scopedFormats.audioData.add(AudioResource(audioUrl, audioMime))
                }
            }
            addAudios()
            addVideos()
        } catch (e: JSONException) {
            logger.error(e.message)
            logger.info(xmlDecoded)
        }
        return scopedFormats
    }

    private fun grabFromJSModsInstance(jsData: JSONObject): Any? {
        if (jsData.toString().isNotBlank()) {
            jsData.getNullableJSONObject("jsmods")?.getNullableJSONArray("instances")?.let {
                return grabVideoData(it)
            }
        }
        return null
    }

    private fun grabVideoData(instance: JSONArray): Any? {
        for (i in 0 until instance.length()) {
            val item = instance.getJSONArray(i)
            if (item.getJSONArray(1).getString(0) == "VideoConfig") {
                val videoDetails = item.getJSONArray(2).getJSONObject(0)
                val videoData = videoDetails.getJSONArray("videoData").getJSONObject(0)
                val dashXml = videoData.getNullableString("dash_manifest")
                dashXml?.let { extractFromDash(it) }

                for (s in arrayOf("hd", "sd")) {
                    val url = videoData.getNullableString("${s}_src")
                    if (url == null || url == "null") continue
                    localFormats.videoData.add(
                        VideoResource(
                            url,
                            MimeType.VIDEO_MP4,
                            videoData.get("original_width")
                                .toString() + "x" + videoData.get("original_height") + "(" + s.uppercase() + ")",

                        )
                    )
                }
                return SUCCESS
            }
        }
        return null
    }

    override suspend fun testWebpage(string: String) {
        onProgress = {}
        scratchWebPage(string)
    }

    companion object {
        const val TAG: String = Statics.TAG.plus(":Facebook")
        const val SUCCESS = -1 // Null if fails
        const val FAILS = 1 // so don't send back the response
        val logger = LoggerFactory.getLogger(Facebook::class.java)
        var PAGELET_REGEX = "(?:pagelet_group_mall|permalink_video_pagelet|hyperfeed_story_id_[0-9a-f]+)".toRegex()
    }
}
