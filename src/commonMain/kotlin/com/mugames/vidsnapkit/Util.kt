package com.mugames.vidsnapkit

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.regex.Pattern
/**
 * @author Udhaya
 * Created on 22-01-2022
 */
class Util {
    companion object {
        /**
         * Used to decode HTML
         *
         * @param text encoded HTML
         * @return decoded HTML and `null` if encoding fails
         */
        fun decodeHTML(text: String?): String? {
            if (text == null) return null
            var decoded: String?
//            decoded = HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
            try {
                return URLDecoder.decode(text, "UTF-8")
            } catch (e: UnsupportedEncodingException) {
                e.printStackTrace()
            }
            return null
        }

        /**
         * To find resolution from given picture URL
         *
         * @param url image URL
         * @return resolution in `width x height` format or `--`
         */
        fun getResolutionFromUrl(url: String): String {
            val matcher = Pattern.compile("([\\d ]{2,5}[x][\\d ]{2,5})").matcher(url)
            if (matcher.find()) return matcher.group(1) ?: "--"
            return "--"
        }

        /**
         * Replaces all white spaces and tabs from title
         *
         * @param name title of video needed to be filtered
         * @return title without newline and white tabs
         */
        fun filterName(name:String) = name.replace("[\n.\t]".toRegex(),"")
    }
}