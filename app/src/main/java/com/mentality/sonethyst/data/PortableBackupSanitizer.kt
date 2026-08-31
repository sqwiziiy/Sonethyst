package com.mentality.sonethyst.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Sanitization used only at the portable backup boundary. */
object PortableBackupSanitizer {
    private val alwaysSensitive = setOf(
        "api_key",
        "access_token",
        "refresh_token",
        "client_token",
    )

    fun artworkUrl(url: String): String {
        val parsed = url.toHttpUrlOrNull() ?: return url
        val queryNames = parsed.queryParameterNames.map { it.lowercase() }.toSet()
        val subsonicAuth = parsed.encodedPath.contains("/rest/", ignoreCase = true) &&
            "t" in queryNames && "s" in queryNames
        if (queryNames.none { it in alwaysSensitive } && !subsonicAuth) return url

        return parsed.newBuilder().apply {
            parsed.queryParameterNames.forEach { name ->
                val lower = name.lowercase()
                if (lower in alwaysSensitive || (subsonicAuth && lower in setOf("u", "t", "s"))) {
                    removeAllQueryParameters(name)
                }
            }
        }.build().toString()
    }

    fun preferenceString(value: String): String {
        val json = runCatching { JsonParser.parseString(value) }.getOrNull()
        return if (json != null && (json.isJsonObject || json.isJsonArray)) {
            sanitizeJson(json).toString()
        } else {
            artworkUrl(value)
        }
    }

    private fun sanitizeJson(element: JsonElement): JsonElement = when {
        element.isJsonObject -> {
            val source = element.asJsonObject
            val result = JsonObject()
            source.entrySet().forEach { (key, child) ->
                result.add(key, sanitizeJson(child))
            }
            result
        }
        element.isJsonArray -> {
            val result = JsonArray()
            element.asJsonArray.forEach { result.add(sanitizeJson(it)) }
            result
        }
        element.isJsonPrimitive && element.asJsonPrimitive.isString ->
            com.google.gson.JsonPrimitive(artworkUrl(element.asString))
        else -> element
    }

    fun playEvent(event: PlayEvent): PlayEvent = event.copy(artworkUrl = artworkUrl(event.artworkUrl))

    fun playEvents(events: List<PlayEvent>): List<PlayEvent> = events.map(::playEvent)
}
