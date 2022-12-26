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

fun String.toJSONObject() = JSONObject(this)

fun String.toJSONArray() = JSONArray(this)