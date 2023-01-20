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

package com.mugames.vidsnapkit.dataholders

import com.mugames.vidsnapkit.MimeType

/**
 * @author Udhaya
 * Created on 16-02-2022
 */

/**
 * Used to store basic details of Video Available for given link
 *
 * @param url Direct URL for the particular video. It will be downloadable
 * @param mimeType Mimetype of the video
 * @param quality Resolution of the video. Eg:1080x1920, Default Value: `--`
 * @param size Size of video in bytes
 * @param hasAudio Specifies does this video has Audio with it.
 */
data class VideoResource(
    val url: String,
    val mimeType: String,
    val quality: String = "--",
    var size: Long = 0,
    val hasAudio: Boolean = true
)

data class ImageResource(
    val url: String,
    val mimeType: String = MimeType.IMAGE_JPEG,
    val resolution: String = "--",
    var size: Long = 0,
)

/**
 * Used to store basic details of Audio Available for given link
 *
 * @param url Direct URL for the particular video. It will be downloadable
 * @param mimeType Mimetype of the video
 * @param size Size of video in bytes
 */
data class AudioResource(
    val url: String,
    val mimeType: String,
    var size: Long = 0,
)
