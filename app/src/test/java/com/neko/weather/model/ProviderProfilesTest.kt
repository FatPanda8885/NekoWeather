package com.neko.weather.model

import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderProfilesTest {
    @Test
    fun includesQWeatherPresetsForEveryContentType() {
        val profiles = defaultProviderProfiles()

        assertTrue(profiles.weather.any { it.id == "builtin-qweather" })
        assertTrue(profiles.air.any { it.id == "builtin-qweather-air" })
        assertTrue(profiles.alerts.any { it.id == "builtin-qweather-alerts" })
    }
}
