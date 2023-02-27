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
    /**
     * It's instance will be returned by Extractor.start() If everything went correct
     * @param formats contains details of given post url
     */
    data class Success(val formats: List<Formats>) : Result()

    /**
     * When the extraction progress goes on
     * @param progressState current state
     */
    @Deprecated("This class is found to be useless after a few year")
    data class Progress(val progressState: ProgressState) : Result()

    /**
     * When error occurs while extraction process
     * @param error tells what is actually went wrong
     */
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
     * Called When Url is empty
     */
    object InvalidUrl : Error()

    /**
     * Called when cookies are null but required by website
     *
     * Sometimes features such as private video downloader requires authentication from the site
     * passing cookies before call to Extractor.start() solves it.
     *
     * For eg:
     * If you use Instagram downloader then you get this error
     * then you need to pass valid Instagram cookies before start() call
     */
    object LoginRequired : Error()

    /**
     * Called when cookies are invalid
     */
    object InvalidCookies : Error()

    /**
     * Report this kind of error to VidSnapKit developer
     */
    class InternalError(message: String, e: Exception? = null) : Error(message, e)

    /**
     * These are minor error happens in cases such as post not found
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
