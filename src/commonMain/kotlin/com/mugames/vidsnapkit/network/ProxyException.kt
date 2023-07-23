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

package com.mugames.vidsnapkit.network

/**
 * @author Udhaya
 * @Created 22-07-2023
 */

/**
 * Sometimes there might be trouble connecting
 * you with the social media's server.
 * If you get this error kindly retry the request
 */
class ProxyException(exception: Exception? = null) :
    IllegalStateException("Unable to process request because of connection problem", exception)
