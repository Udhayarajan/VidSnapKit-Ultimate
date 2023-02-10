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

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * @author Udhaya
 * Created on 22-01-2022
 */
fun JSONObject.getNullableJSONObject(name: String): JSONObject? = try {
    getJSONObject(name)
} catch (e: JSONException) {
    null
}

fun JSONArray.getNullableJSONObject(index: Int): JSONObject? = try {
    getJSONObject(index)
} catch (e: JSONException) {
    null
}

fun JSONArray.getNullableJSONArray(index: Int): JSONArray? = try {
    getJSONArray(index)
} catch (e: JSONException) {
    null
}

fun JSONObject.getNullableJSONArray(name: String): JSONArray? = try {
    getJSONArray(name)
} catch (e: JSONException) {
    null
}

fun JSONObject.getNullableString(name: String): String? = try {
    getString(name)
} catch (e: JSONException) {
    null
}

fun JSONObject.getNullable(name: String): String? = try {
    get(name).toString()
} catch (e: JSONException) {
    null
}

fun String.toJSONObject() = JSONObject(this)

fun String.toJSONArray() = JSONArray(this)

fun String.toJSONObjectOrNull() = try {
    toJSONObject()
} catch (e: JSONException) {
    null
}

fun String.toJSONArrayOrNull() = try {
    toJSONArray()
} catch (e: JSONException) {
    null
}