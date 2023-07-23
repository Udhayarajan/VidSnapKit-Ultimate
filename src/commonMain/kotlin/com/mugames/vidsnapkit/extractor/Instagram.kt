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
import io.ktor.http.*
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.util.*
import java.util.regex.Pattern

/**
 * @author Udhaya
 * Created on 16-02-2022
 */
class Instagram internal constructor(url: String) : Extractor(url) {
    companion object {
        const val TAG: String = Statics.TAG.plus(":Instagram")
        const val STORIES_URL = "https://www.instagram.com/stories/%s/?__a=1&__d=dis"
        const val STORIES_API = "https://i.instagram.com/api/v1/feed/user/%s/story/"
        const val PROFILE_API = "https://www.instagram.com/%s/?__a=1&__d=dis"
        const val POST_API = "https://i.instagram.com/api/v1/media/%s/info/"
        const val NO_VIDEO_STATUS_AVAILABLE = "No video Status available"
        const val NO_STATUS_AVAILABLE = "No stories Available to Download"
        const val HIGHLIGHTS_API = "https://www.instagram.com/api/v1/feed/reels_media/?reel_ids=%s"
        const val GRAPHQL_URL =
            "https://www.instagram.com/graphql/query/?query_hash=%s&variables={\"shortcode\":\"%s\"}&__a=1&__d=dis"
        const val DEFAULT_QUERY_HASH = "b3055c01b4b222b8a47dc12b090e4e64"
        const val AUDIO_API = "https://www.instagram.com/api/v1/clips/music/"
        private val logger = LoggerFactory.getLogger(Instagram::class.java)
    }

    private val formats = Formats()
    private var isReel = false

    private suspend fun isCookieValid(): Boolean {
        if (cookies.isNullOrEmpty()) return false
        val res = httpRequestService
            .getRawResponse(
                "https://www.instagram.com/accounts/login/",
                headers,
                false
            ) ?: return false
        logger.info("status code=${res.status.value} & http response=$res")
        if (res.status == HttpStatusCode.Found) {
            val newLoc = res.headers["location"].toString()
            logger.info("new loc = $newLoc")
            val restrictedKeywords = listOf("privacy/checks", "challenge", "coig_restricted", "accounts/login")
            val containsRestrictedKeyword = restrictedKeywords.any { keyword ->
                newLoc.contains(keyword, ignoreCase = true)
            }

            if (newLoc == "https://www.instagram.com/" || !containsRestrictedKeyword) {
                logger.info("valid cookie")
                return true
            }
        }
        // if redirection is not set
        if (res.status == HttpStatusCode.OK) {
            if (res.call.request.url.toString() == "https://www.instagram.com/") {
                logger.info("valid cookie")
                return true
            }
        }
        logger.info("Oops!, Cookie is invalid so removing it from header")
        return false
    }

    private fun getShortcode(): String? {
        val matcher = Pattern.compile("(?:reel|reels|p)/(.*?)[/?]").matcher(inputUrl)
        return if (matcher.find()) matcher.group(1) else {
            logger.error("unable to find shortcode from the url=$inputUrl")
            null
        }
    }

    private fun getMediaId(page: String? = null): String? {
        if (page == null) {
            val matcher = Pattern.compile("/([0-9]{19})(?:/|)").matcher(inputUrl)
            return if (matcher.find()) matcher.group(1)
            else null
        }
        val matcher = Pattern.compile("\"media_id\":\"?(.*?)[\",_]").matcher(page)
        return if (matcher.find()) matcher.group(1) else getShortcode()?.run {
            shortcodeToMediaID(this)
        }
    }

    private fun getAudioID(): String {
        val matcher = Pattern.compile("/reels?/audio/([0-9].*?)/").matcher(inputUrl)
        if (matcher.find())
            return matcher.group(1)
        throw Exception("unable to get audio ID")
    }

    private fun isPostUrl(): Boolean {
        if (inputUrl.contains("/p/")) return true
        return inputUrl.contains("(/reel/|/tv/|/reels/)[\\w-]{11}".toRegex())
    }

    private fun isHighlightsPost(): Boolean {
        return inputUrl.contains("stories/highlights/".toRegex())
    }

    private fun getHighlightsId(): String? {
        val matcher =
            Pattern.compile("(?:https|http)://(?:www\\.|.*?)instagram.com/(?:stories/highlights/|)([A-Za-z0-9_.]+)")
                .matcher(inputUrl)
        return if (matcher.find()) matcher.group(1)
        else null
    }

    private fun isAccessible(jsonObject: JSONObject, scarpingProfileName: String? = null): Boolean {
        val user = jsonObject.getJSONObject("graphql").getJSONObject("user")
        val isBlocked = user.getBoolean("has_blocked_viewer")
        val isPrivate = user.getBoolean("is_private")
        val followedByViewer = user.getBoolean("followed_by_viewer")
        val isSameUser =
            user.getString("username") == scarpingProfileName // When user tires to download their own story
        return isSameUser || ((isPrivate && followedByViewer) || !isPrivate) && !isBlocked
    }

    private fun getUserName(): String? {
        val matcher = Pattern.compile("(?:https|http)://(?:www\\.|.*?)instagram.com/(?:stories/|)([A-Za-z0-9_.]+)")
            .matcher(inputUrl)
        return if (matcher.find()) matcher.group(1)
        else null
    }

    private suspend fun getUserID(): String? = cookies?.let { _ ->
        getUserName()?.let {
            val res = httpRequestService.getResponse(String.format(PROFILE_API, it), headers) ?: run {
                clientRequestError()
                return null
            }
            val response = res.toJSONObjectOrNull() ?: run {
                loginRequired()
                return null
            }
            if (response.toString() == "{}") {
                clientRequestError()
                return null
            }
            if (!isAccessible(response, it)) {
                onProgress(Result.Failed(Error.InvalidCookies))
                return null
            }
            return response.getJSONObject("graphql")?.getJSONObject("user")?.getString("id")
        } ?: run {
            onProgress(Result.Failed(Error.InvalidUrl))
        }
        return null
    } ?: run {
        loginRequired()
        null
    }

    private suspend fun extractMusicAssetInfo(assetInfo: JSONObject) {
        formats.title =
            assetInfo.getNullableString("title")?.ifEmpty { null } ?: assetInfo.getNullableString("subtitle")
            ?.ifEmpty { null } ?: "Reels_audio ${assetInfo.getNullableString("display_artist")}"
        val imageUrl = assetInfo.run {
            getNullableString("cover_artwork_uri")?.ifEmpty { null } ?: getNullableString("cover_artwork_thumbnail_uri")
                ?: getJSONObject("music_composition_info").getString("placeholder_profile_pic_url")
        }
        formats.imageData.add(ImageResource(imageUrl, Util.getResolutionFromUrl(imageUrl)))

        assetInfo.run {
            getNullableString("progressive_download_url")
                ?: getString("fast_start_progressive_download_url")
        }.let {
            formats.audioData.add(AudioResource(it, MimeType.AUDIO_MP4))
        }

        assetInfo.getNullableString("web_30s_preview_download_url")?.let {
            formats.audioData.add(AudioResource(it, MimeType.AUDIO_MP4))
        }

        videoFormats.add(formats)
        finalize()
    }

    override suspend fun analyze(payload: Any?) {
        formats.src = "Instagram"
        formats.url = inputUrl

        val load = payload as? HashMap<*, *>

        isReel = inputUrl.contains("/reel/") || inputUrl.contains("/reels/")

        inputUrl = inputUrl.replace("/reels/", "/reel/")
        if (!inputUrl.endsWith("/")) inputUrl = "$inputUrl/"
        inputUrl = "${inputUrl.replace("/[^/?]*\$|/*\\?.*\$".toRegex(), "")}/?img_index=1"

        if (isPostUrl()) {
            if (!isCookieValid()) {
                cookies = null
            }
            if (load?.get("forced") == true && cookies != null) {
                logger.info("direct ex as forced")
                directExtraction()
                return
            }
            when (val res = httpRequestService.getResponse(inputUrl, headers)) {
                null -> {
                    clientRequestError()
                    return
                }

                "429" -> {
                    logger.info("direct ex as 429 unsafe, cookies not validated")
                    directExtraction()
                }

                else -> extractInfoShared(res)
            }
        } else if (isHighlightsPost()) {
            headers["User-Agent"] =
                "Mozilla/5.0 (iPhone; CPU iPhone OS 12_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Instagram 105.0.0.11.118 (iPhone11,8; iOS 12_3_1; en_US; en-US; scale=2.00; 828x1792; 165586599)"
            val highlightsId = getHighlightsId()
            highlightsId?.let {
                extractHighlights(it)
            }
        } else if (inputUrl.contains("audio")) {
            inputUrl = inputUrl.replace("/reel/", "/reels/")
            val audioID = getAudioID()

            val audioPayload = Hashtable<String, Any>()
            audioPayload["audio_cluster_id"] = audioID
            audioPayload["original_sound_audio_asset_id"] = audioID
            headers["X-Instagram-Ajax"] = "1007887313"

            val pre = httpRequestService.postRawResponse(AUDIO_API, headers, audioPayload) ?: run {
                loginRequired()
                return
            }
            var appId = "936619743392459"

            withTimeoutOrNull(2000) {
                httpRequestService.getResponse(inputUrl, headers)?.let {
                    val appIdRegex = listOf(
                        "\"app_id\":\"(\\d.*?)\"".toRegex(),
                        "\"appId\":\"(\\d.*?)\"".toRegex(),
                        "\"APP_ID\":\"(\\d.*?)\"".toRegex(),
                        "\"X-IG-App-ID\":\"(.*?)\"".toRegex()
                    )
                    for (regex in appIdRegex) {
                        val matcher = regex.find(it)
                        matcher?.groups?.get(1)?.let {
                            appId = it.value
                        }
                    }
                }
            }

            val tempHeader = headers.clone() as Hashtable<String, String>
            pre.headers.getAll("set-cookie")?.forEach {
                val (k, v) = it.split(";")[0].split("=")
                if (k.contains("csrftoken", ignoreCase = true)) {
                    tempHeader["X-Csrftoken"] = v
                }
            }
            tempHeader.remove("User-Agent")
            tempHeader["X-Ig-App-Id"] = appId
            val res = httpRequestService.postRequest(AUDIO_API, tempHeader, audioPayload)
            val metadata = res?.toJSONObject()?.getJSONObject("metadata")
            metadata?.run {
                getNullableJSONObject("original_sound_info")?.let { extractFromOriginalAudioInfo(it) }
                    ?: getNullableJSONObject("music_info")
                        ?.getJSONObject("music_asset_info")
                        ?.also { extractMusicAssetInfo(it) }
                    ?: run {
                        missingLogic()
                        return
                    }
            } ?: run {
                clientRequestError()
                return
            }
        } else {
            // possibly user url
            headers["User-Agent"] =
                "Mozilla/5.0 (iPhone; CPU iPhone OS 12_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 Instagram 105.0.0.11.118 (iPhone11,8; iOS 12_3_1; en_US; en-US; scale=2.00; 828x1792; 165586599)"
            val userId = getUserID()
            userId?.let {
                extractStories(it)
            }
        }
    }

    private suspend fun extractFromOriginalAudioInfo(soundInfo: JSONObject) {
        formats.title = soundInfo.getString("original_audio_title")
        val audioUrl = soundInfo.getString("progressive_download_url")
        val imageURL = soundInfo.getJSONObject("ig_artist").getString("profile_pic_url")
        formats.imageData.add(ImageResource(imageURL, Util.getResolutionFromUrl(imageURL)))
        formats.audioData.add(AudioResource(audioUrl, MimeType.AUDIO_MP4))
        videoFormats.add(formats)
        finalize()
    }

    private suspend fun directExtraction() {
        inputUrl = inputUrl.replace("/reels/", "/p/")
        inputUrl = inputUrl.replace("/reel/", "/p/")
        inputUrl = inputUrl.replace("https://instagram.com", "https://www.instagram.com")
        logger.info("The new url is $inputUrl&__a=1&__d=dis")
        val res = httpRequestService.getResponse(inputUrl.plus("&__a=1&__d=dis"), headers) ?: run {
            loginRequired()
            return
        }
        if (res == "429" && isCookieValid()) {
            val mediaID = shortcodeToMediaID(getShortcode())
            mediaID?.let {
                val items =
                    httpRequestService
                        .getResponse(
                            POST_API.format(shortcodeToMediaID(getShortcode())),
                            headers
                        )?.let {
                            it.toJSONObjectOrNull()?.getNullableJSONArray("items") ?: run {
                                loginRequired()
                                return
                            }
                        } ?: run {
                        loginRequired()
                        return
                    }
                extractFromItems(items)
                return
            } ?: run {
                logger.error("unable to find mediaID for url $inputUrl")
                loginRequired()
                return
            }
        }
        extractFromItems(
            res.toJSONObjectOrNull()?.getNullableJSONArray("items") ?: run {
                loginRequired()
                return
            }
        )
    }

    private suspend fun extractHighlights(highlightsId: String, isStory: Boolean = false) {
        val highlights = httpRequestService.getResponse(
            HIGHLIGHTS_API.format(if (!isStory) "highlight%3A$highlightsId" else highlightsId),
            headers
        )?.toJSONObjectOrNull()
        highlights?.let {
            if (it.getNullable("login_required") == "true") {
                loginRequired()
                return
            }
            val highlight = it.getJSONObject("reels").run {
                getNullableJSONObject("highlight:$highlightsId") ?: getNullableJSONObject(highlightsId) ?: run {
                    loginRequired()
                    return
                }
            }

            formats.title =
                highlight.getNullableString("title") ?: "${if (isStory) "stories" else "highlight"}:$highlightsId"
            extractFromItems(highlight.getJSONArray("items"))
        } ?: clientRequestError()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun extractStories(userId: String) {
        val stories = httpRequestService.getResponse(STORIES_API.format(userId), headers)
        val reel = stories?.toJSONObjectOrNull()?.getNullableJSONObject("reel")
        reel?.let {
            extractFromItems(it.getJSONArray("items"))
        } ?: extractHighlights(userId, true)
    }

    private suspend fun extractInfoShared(page: String) {
        if (page == "{error:\"Invalid Cookies\"}") {
            logger.info("direct ex as Invalid cookie arises in extractInfoShared()")
            directExtraction()
            return
        }
        suspend fun newApiRequest() {
            val mediaId = getMediaId(page)
            try {
                if (mediaId == null) throw JSONException("mediaId is null purposely thrown wrong error")
                val url = POST_API.format(mediaId)
                extractFromItems(
                    httpRequestService
                        .getResponse(url, headers)
                        .toString()
                        .toJSONObject()
                        .getJSONArray("items")
                )
            } catch (e: JSONException) {
                loginRequired()
                return
            }
        }

        onProgress(Result.Progress(ProgressState.Start))
        val pattern = Pattern.compile("window\\._sharedData\\s*=\\s*(\\{.+?\\});")
        val matcher = pattern.matcher(page)
        val jsonString = if (matcher.find()) {
            matcher.group(1)
        } else {
            val json = getFromBrutForcing(page)
            if (json != null) {
                brutForcedExtraction(
                    json.toJSONObjectOrNull() ?: run {
                        json.toJSONArray().getJSONObject(0)
                    }
                )
                return
            } else {
                logger.info("finally calling direct ex in unsafe, cookies are not validated")
                tryWithQueryHash(page)
            }
            return
        }
        val jsonObject = JSONObject(jsonString)
        val postPage: JSONArray? = jsonObject.getNullableJSONObject("entry_data")?.getNullableJSONArray("PostPage")

        postPage?.let { post ->
            val zero: JSONObject = post.getJSONObject(0)
            val graphql: JSONObject? = zero.getNullableJSONObject("graphql")
            val media = graphql?.getNullableJSONObject("shortcode_media") ?: zero.getNullableJSONObject("media")
            media?.let {
                setInfo(it)
            } ?: run {
                extractInfoAdd(page)
            }
        } ?: run {
            fun isObjectPresentInEntryData(objectName: String): Boolean {
                return jsonObject.getNullableJSONObject("entry_data")?.getNullableJSONArray(objectName) != null
            }
            if (isObjectPresentInEntryData("LoginAndSignupPage")) {
                loginRequired()
            } else if (isObjectPresentInEntryData("HttpErrorPage")) {
                onProgress(Result.Failed(Error.Instagram404Error(cookies != null)))
            } else {
                val user0 = jsonObject.getJSONObject("entry_data").getNullableJSONArray("ProfilePage")?.getJSONObject(0)
                    ?: run {
                        newApiRequest()
                        return
                    }
                if (!isAccessible(user0)) onProgress(Result.Failed(Error.InvalidCookies))
                else onProgress(Result.Failed(Error.InternalError("can't find problem")))
            }
        }
    }

    private suspend fun brutForcedExtraction(jsonObject: JSONObject) {
        formats.title = jsonObject.getString("articleBody").ifEmpty { jsonObject.getString("headline") }
        var isMultiple = true
        val images = jsonObject.getJSONArray("image")
        for (i in 0 until images.length()) {
            val image = images.getJSONObject(i)
            if (images.length() == 1) {
                isMultiple = false
                formats.imageData.add(
                    ImageResource(
                        url = image.getString("url"),
                        resolution = image.getString("width") + "x" + image.getString("height")
                    )
                )
                continue
            }
            val localFormat = formats.copy(title = "", imageData = mutableListOf(), videoData = mutableListOf())
            localFormat.title = image.getString("caption").ifEmpty {
                (image.getNullableString("name") ?: "").ifEmpty {
                    image.getNullableString("description")
                        ?: (formats.title.ifEmpty { "Instagram Image Carousel " } + i)
                }
            }
            localFormat.imageData.add(
                ImageResource(
                    url = image.getString("url"),
                    resolution = image.getString("width") + "x" + image.getString("height")
                )
            )
            videoFormats.add(localFormat)
        }

        val videos = jsonObject.getJSONArray("video")
        for (i in 0 until videos.length()) {
            val video = videos.getJSONObject(i)
            if (video.length() == 1) {
                isMultiple = false
                formats.videoData.add(
                    VideoResource(
                        url = video.getString("contentUrl"),
                        mimeType = MimeType.VIDEO_MP4,
                        quality = video.getString("width") + "x" + video.getString("height")
                    )
                )
                formats.imageData.add(ImageResource(url = video.getString("thumbnailUrl")))
                continue
            }
            val localFormat = formats.copy(title = "", imageData = mutableListOf(), videoData = mutableListOf())
            localFormat.title = video.getString("caption").ifEmpty {
                (video.getNullableString("name") ?: "").ifEmpty {
                    video.getNullableString("description")
                        ?: (formats.title.ifEmpty { "Instagram Video Carousel " } + i)
                }
            }
            localFormat.imageData.add(ImageResource(url = video.getString("thumbnailUrl")))
            localFormat.videoData.add(
                VideoResource(
                    url = video.getString("contentUrl"),
                    mimeType = MimeType.VIDEO_MP4,
                    quality = video.getString("width") + "x" + video.getString("height")
                )
            )
            videoFormats.add(localFormat)
        }

        if (!isMultiple) videoFormats.add(formats)
        finalize()
    }

    private fun getFromBrutForcing(page: String): String? {
        val matcher = Pattern.compile("<script type=\"application/ld\\+json\".*?>(.*?)</script>").matcher(page)
        if (matcher.find()) {
            val res = matcher.group(1)
            if (res.contains("articleBody")) {
                return res
            }
        }
        return null
    }

    private suspend fun getQueryHash(js: String): List<String> {
        val list = mutableListOf<String>()
        val matcher = Pattern.compile("\\w=\"([a-z0-9]{32})\";").matcher(js)
        while (matcher.find()) {
            list.add(matcher.group(1))
        }
        return list
    }

    private suspend fun getQueryHashFromAllJSInPage(page: String): MutableList<String>? {
        val ids = mutableListOf<String>()
        val matcher = Pattern.compile("<link rel=\"preload\" href=\"(.*?)\" as=\"script\"").matcher(page)
        while (matcher.find()) {
            ids.addAll(
                getQueryHash(
                    httpRequestService.getResponse(matcher.group(1), headers) ?: run {
                        return null
                    }
                )
            )
        }
        return ids
    }

    private suspend fun setInfo(media: JSONObject) {
        onProgress(Result.Progress(ProgressState.Middle))
        var videoName: String? = media.getNullableString("title")

        if (videoName == null || videoName == "null" || videoName.isEmpty()) videoName =
            media.getNullableJSONObject("edge_media_to_caption")?.getNullableJSONArray("edges")
                ?.getNullableJSONObject(0)?.getJSONObject("node")?.getString("text")
        if (videoName == null || videoName == "null" || videoName.isEmpty()) videoName = "instagram_video"
        formats.title = Util.filterName(videoName)
        val fileURL: String? = media.getNullableString("video_url")
        fileURL?.let { url ->
            formats.videoData.add(
                VideoResource(
                    url, MimeType.VIDEO_MP4
                )
            )
            formats.imageData.add(
                ImageResource(
                    resolution = Util.getResolutionFromUrl(
                        media.getString(
                            "thumbnail_src"
                        )
                    ),
                    url = media.getString("thumbnail_src")
                )
            )
            videoFormats.add(formats)
        } ?: run {
            val edges: JSONArray? =
                media.getNullableJSONObject("edge_sidecar_to_children")?.getNullableJSONArray("edges")

            edges?.let { edgesObj ->
                for (i in 0 until edgesObj.length()) {
                    val format = formats.copy(videoData = mutableListOf())
                    val node = edgesObj.getJSONObject(i).getJSONObject("node")
                    if (node.getBoolean("is_video")) {
                        format.imageData.add(
                            ImageResource(
                                resolution = Util.getResolutionFromUrl(node.getString("display_url")),
                                url = node.getString("display_url")
                            )
                        )
                        format.videoData.add(
                            VideoResource(
                                node.getString("video_url"), MimeType.VIDEO_MP4
                            )
                        )
                    }
                    videoFormats.add(format)
                }
            } ?: run {
                onProgress(
                    Result.Failed(
                        Error.InternalError(
                            "Media not found", Exception("$media")
                        )
                    )
                )
            }
        }

        finalize()
    }

    private suspend fun extractInfoAdd(page: String) {
        val pattern = Pattern.compile("window\\.__additionalDataLoaded\\s*\\(\\s*[^,]+,\\s*(\\{.+?\\})\\s*\\)\\s*;")
        val matcher = pattern.matcher(page)
        val jsonString = if (matcher.find()) {
            matcher.group(1)
        } else null

        if (jsonString.isNullOrEmpty()) {
            logger.info("trying with query hash")
            tryWithQueryHash(page)
            return
        }
        val jsonObject = JSONObject(jsonString)
        val graphql: JSONObject? = jsonObject.getNullableJSONObject("graphql")
        graphql?.let {
            val media = it.getNullableJSONObject("shortcode_media")
            media?.let { mediaIt ->
                setInfo(mediaIt)
            } ?: run {
                onProgress(Result.Failed(Error.InternalError("MediaNotFound")))
            }
        } ?: run {
            extractFromItems(jsonObject.getJSONArray("items"))
        }
    }

    private suspend fun tryWithQueryHash(page: String) {
        val queryHash = withTimeoutOrNull(2000) {
            getQueryHashFromAllJSInPage(page)
        } ?: DEFAULT_QUERY_HASH
        val res = httpRequestService.getResponse(GRAPHQL_URL.format(queryHash, getShortcode()), headers)
        logger.info("graphQL response = $res")
        val shortcodeMedia =
            res?.toJSONObject()?.getJSONObject("data")?.getNullableJSONObject("shortcode_media") ?: run {
                logger.info("unable to even get from graphQL so trying direct ex")
                directExtraction()
                return
            }
        setInfo(shortcodeMedia)
    }

    private suspend fun extractFromItems(items: JSONArray) {
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)

            if (formats.title.isEmpty()) {
                onProgress(Result.Progress(ProgressState.Middle))
                val caption = item.getNullableJSONObject("caption")
                formats.title = Util.filterName(
                    caption?.getNullableString("text") ?: run {
                        item.getNullableString("caption") ?: "Instagram_Reels"
                    }
                )
            }

            val videoVersion = item.getNullableJSONArray("video_versions")
            videoVersion?.let {
                val format = formats.copy(videoData = mutableListOf(), imageData = mutableListOf())
                for (j in 0 until it.length()) {
                    val video = it.getJSONObject(j)
                    format.videoData.add(
                        VideoResource(
                            video.getString("url"), MimeType.VIDEO_MP4,
                            try {
                                video.getString("width")
                            } catch (e: JSONException) {
                                video.getInt("width").toString()
                            } + "x" + try {
                                video.getString("height")
                            } catch (e: JSONException) {
                                video.getInt("height").toString()
                            }
                        )
                    )
                }
                val imageVersion2 = item.getJSONObject("image_versions2")
                val candidates = imageVersion2.getJSONArray("candidates")
                val thumbnailUrl = candidates.getJSONObject(0).getString("url")
                format.imageData.add(
                    ImageResource(
                        resolution = Util.getResolutionFromUrl(thumbnailUrl), url = thumbnailUrl
                    )
                )
                videoFormats.add(format)
            } ?: run {
                item.getNullableJSONArray("carousel_media")?.let {
                    extractFromItems(it)
                    return
                } ?: run {
                    val imageVersion2 = item.getNullableJSONObject("image_versions2")
                    imageVersion2?.let {
                        val format = formats.copy(imageData = mutableListOf())

                        val candidates = it.getJSONArray("candidates")
                        val thumbnailUrl = candidates.getJSONObject(0).getString("url")
                        format.imageData.add(
                            ImageResource(
                                resolution = Util.getResolutionFromUrl(thumbnailUrl), url = thumbnailUrl
                            )
                        )
                        videoFormats.add(format)
                    }
                }
            }
        }
        if (!isPostUrl() && videoFormats.isEmpty()) {
            onProgress(Result.Failed(Error.NonFatalError(NO_VIDEO_STATUS_AVAILABLE)))
        } else finalize()
    }

    private fun shortcodeToMediaID(shortcode: String?): String? {
        if (shortcode == null) return null
        var id = BigInteger.ZERO
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

        for (i in shortcode.indices) {
            val char = shortcode[i]
            val charIndex = alphabet.indexOf(char)
            id = id * BigInteger.valueOf(64) + BigInteger.valueOf(charIndex.toLong())
        }

        return id.toString()
    }

    override suspend fun testWebpage(string: String) {
        onProgress = {
            println(it)
        }
        extractInfoShared(string)
    }
}
