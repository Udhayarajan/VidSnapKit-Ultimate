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

/**
 * @author Udhaya
 * Created on 23-01-2022
 */
class MimeType {
    /**
     * Mimetype of basic media files that will be used across
     * this library
     */
    companion object {
        /**
         * If it is .mp4 format
         *
         * Constant Value: "video/mp4"
         */
        const val VIDEO_MP4 = "video/mp4"

        /**
         * If it is .webm
         *
         * Constant Value: "video/webm"
         */
        const val VIDEO_WEBM = "video/webm"

        /**
         * If it is .aac and this type can be combined with .mp4 files
         *
         * Constant Value: "audio/mp4"
         */
        const val AUDIO_MP4 = "audio/mp4"

        /**
         * If it is webm audio and this type can be combined with .webm files
         *
         * Constant Value: "audio/webm"
         */
        const val AUDIO_WEBM = "audio/webm"

        /**
         * If it is .jpg or /jpeg file type
         *
         * Constant Value: "image/jpeg"
         */
        const val IMAGE_JPEG = "image/jpeg"

        /**
         * It is of .m3u8 type
         *
         * Constant Value: "application/x-mpegURL"
         */
        const val APPLICATION_X_MPEG_URL = "application/x-mpegURL";

        fun fromCodecs(codec: String, defaultCodec: String = "") = with(codec) {
            when {
                contains("mp4a", ignoreCase = true) -> AUDIO_MP4
                contains("opus", ignoreCase = true) -> AUDIO_WEBM
                contains("avc", ignoreCase = true) -> VIDEO_MP4
                contains("vp8", ignoreCase = true) || contains("vp9", ignoreCase = true) -> MimeType.VIDEO_WEBM
                else ->{
                    println("Unable to find mimetype from codec $codec")
                    defaultCodec
                }
            }
        }
    }
}