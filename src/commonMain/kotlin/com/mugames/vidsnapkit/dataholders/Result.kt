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

/**
 * @author Udhaya
 * Created on 21-01-2022
 */

/**
 * It contains callbacks what's happening in Extraction Process
 *
 */
sealed class Result {
    data class Success(val formats: List<Formats>) : Result()
    data class Progress(val progressState: ProgressState) : Result()
    data class Failed(val error: Error) : Result()
}

/**
 * Instance of [Error] will be returned when [Result.Failed]
 *
 * @param message Message what went wrong, Default Value: `null`
 * @param e It provides traceback, Default Value: `null`
 */
sealed class Error(val message: String? = null, val e: Exception? = null) {
    /**
     * Called when Internet connection is not available
     */
    object NetworkError : Error()

    /**
     * Called When Url is empty
     */
    object InvalidUrl : Error()

    /**
     * Called when cookies are null but required by website
     */
    object LoginInRequired : Error()

    /**
     * Called when cookies are invalid
     */
    object InvalidCookies : Error()

    /**
     * Report this kind of error to VidSnapKit developer
     */
    class InternalError(message: String, e: Exception? = null) : Error(message, e)

    /**
     * These are minor error happens video not found etc
     */
    class NonFatalError(message: String) : Error(message)

    /**
     * Sometimes unexpectedly instagram returns 404 ERROR
     * even for public post
     */
    class Instagram404Error(val isCookiesUsed: Boolean) : Error()

    /**
     * The current extractor missing some of its extraction logic
     * for the given url
     */
    object MethodMissingLogic: Error("Missing implementation logic")
}

/**
 * Just to show progress to UI/UX
 */
sealed class ProgressState {
    object Start : ProgressState()
    object Middle : ProgressState()
    object End : ProgressState()
}
