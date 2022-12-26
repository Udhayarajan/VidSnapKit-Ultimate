package com.mugames.vidsnapkit.extractor

import com.mugames.vidsnapkit.Util
import com.mugames.vidsnapkit.dataholders.*
import com.mugames.vidsnapkit.network.HttpRequest
import org.json.JSONArray
import java.util.regex.Pattern

/**
 * @author Udhaya
 * Created on 24-05-2022
 */

class LinkedIn internal constructor(url: String) : Extractor(url) {
    private val formats = Formats()

    override suspend fun analyze() {
        formats.url = inputUrl
        formats.src = "LinkedIn"
        onProgress(Result.Progress(ProgressState.Start))
        scratchWebpage(HttpRequest(inputUrl).getResponse())
    }

    private suspend fun scratchWebpage(page: String) {
        val matcher = Pattern.compile("data-sources=\"(.*?)\"").matcher(page)
        if (matcher.find()) {
            fun findTitle(vararg regexes: Regex, defaultTitle: String):String{
                for (regex in regexes){
                    val m = Pattern.compile(regex.toString()).matcher(page)
                    if (m.find()){
                        return m.group(1)?:defaultTitle
                    }
                }
                return defaultTitle
            }
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
            formats.title = findTitle(
                Regex("\"twitter:title\"\\s*content\\s*=\\s*\"(.*?)\">"),
                Regex("\"og:title\"\\s*content\\s*=\\s*\"(.*?)\">"),
                Regex("\"twitter:description\"\\s*content\\s*=\\s*\"([\\w\\W]*?)\">"),
                Regex("\"og:description\"\\s*content\\s*=\\s*\"([\\w\\W]*?)\">"),
                defaultTitle = "LinkedIn_Video"
            )
            getThumbnailData(page)
            videoFormats.add(formats)
            finalize()
        }
    }

    private fun getThumbnailData(page: String){
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