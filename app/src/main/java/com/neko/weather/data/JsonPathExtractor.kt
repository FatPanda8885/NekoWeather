package com.neko.weather.data

import org.json.JSONArray
import org.json.JSONObject

object JsonPathExtractor {
    private val segmentPattern = Regex("([^\\[\\]]+)|\\[(\\d+)]")

    fun get(root: Any, path: String): Any? {
        if (path.isBlank()) return null
        var current: Any? = root
        val segments = path.split('.').flatMap { token ->
            segmentPattern.findAll(token).map { match ->
                match.groups[1]?.value ?: match.groups[2]!!.value.toInt()
            }.toList()
        }

        for (segment in segments) {
            current = when {
                segment is String && current is JSONObject -> current.opt(segment)
                segment is Int && current is JSONArray -> current.opt(segment)
                else -> null
            }
            if (current == null || current == JSONObject.NULL) return null
        }
        return current
    }

    fun number(root: Any, path: String): Double? = when (val value = get(root, path)) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }
}

