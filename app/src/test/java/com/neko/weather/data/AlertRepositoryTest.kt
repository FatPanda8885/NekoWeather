package com.neko.weather.data

import okhttp3.OkHttpClient
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertRepositoryTest {
    private val repository = AlertRepository(OkHttpClient())

    @Test
    fun parsesNwsGeoJsonAlerts() {
        val json = JSONObject(
            """
            {
              "features": [{
                "properties": {
                  "headline": "Severe Thunderstorm Warning",
                  "severity": "Severe",
                  "description": "Strong storms are occurring.",
                  "instruction": "Move indoors.",
                  "expires": "2026-07-16T18:00:00-04:00"
                }
              }]
            }
            """.trimIndent(),
        )

        val alert = repository.parseNws(json).single()

        assertEquals("Severe Thunderstorm Warning", alert.title)
        assertEquals("严重", alert.severity)
        assertTrue(alert.description.contains("Move indoors."))
        assertEquals("美国国家气象局 NWS", alert.source)
    }

    @Test
    fun matchesOnlyPointsInsideCapPolygon() {
        val polygon = "50.0,8.0 50.0,9.0 51.0,9.0 51.0,8.0 50.0,8.0"

        assertTrue(repository.containsPoint(polygon, 50.5, 8.5))
        assertFalse(repository.containsPoint(polygon, 52.0, 8.5))
    }
}
