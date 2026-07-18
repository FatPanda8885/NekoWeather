package com.neko.weather.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonPathExtractorTest {
    @Test
    fun readsNestedFieldsAndArrays() {
        val json = JSONObject("""{"current":{"temperature":23.5},"days":[{"code":2}]}""")

        assertEquals(23.5, JsonPathExtractor.number(json, "current.temperature")!!, 0.0)
        assertEquals(2, JsonPathExtractor.get(json, "days[0].code"))
        assertNull(JsonPathExtractor.get(json, "days[1].code"))
    }
}

