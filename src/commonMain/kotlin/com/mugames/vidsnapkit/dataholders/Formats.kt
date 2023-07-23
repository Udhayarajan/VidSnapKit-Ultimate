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

import io.ktor.http.*

/**
 * @author Udhaya
 * Created on 21-01-2022
 *
 * For a single video it's properties
 * are listed in this class
 *
 * @param title Video title without \n and \t
 * @param url User pasted source URL
 * @param src Which Extractor is used Values can be "INSTAGRAM" or "FACEBOOK"
 *
 * @param imageData List of possible image with it's URL
 * @param videoData List of available video qualities
 * @param audioData List of available audio qualities
 *
 * @param cookies Map of cookies currently used by TikTok downloader. Use this to download video
 *
 * @param selectedVideoIndex Index of selected video and `null` if nothing is selected
 * @param selectedAudioIndex Index of selected audio and `null` if nothing is selected
 * @param selectedThumbnailIndex Resolution of image(key) that was selected
 */
data class Formats(
    var title: String = "",
    var url: String = "",
    var src: String = "",

    val videoData: MutableList<VideoResource> = mutableListOf(),

    val imageData: MutableList<ImageResource> = mutableListOf(),

    val audioData: MutableList<AudioResource> = mutableListOf(),

    val cookies: MutableList<Cookie> = mutableListOf(),

    // Flag to keep remember what index is selected
    var selectedVideoIndex: Int? = null,
    var selectedAudioIndex: Int? = null,
    var selectedThumbnailIndex: Int? = null,
) {
    /**
     * Used to get [VideoResource] of selected video index
     *
     * @return Video at [selectedVideoIndex] if `null` then 1st video will be returned
     */
    fun getSelectedVideo(): VideoResource = videoData[selectedVideoIndex ?: 0]

    /**
     * Used to get [AudioResource] of selected audio index
     *
     * @return Audio at [selectedAudioIndex] if `null` then 1st audio will be returned
     */
    fun getSelectedAudio(): AudioResource = audioData[selectedAudioIndex ?: 0]

    /**
     * Used to get thumbnail URL of selected thumbnail quality
     *
     * @return [Pair] of quality and URL for selected [selectedThumbnailIndex]
     * if `null` then 1st thumbnail will be returned
     */
    fun getSelectedThumbnailUrl(): ImageResource = imageData[selectedThumbnailIndex ?: 0]
}
