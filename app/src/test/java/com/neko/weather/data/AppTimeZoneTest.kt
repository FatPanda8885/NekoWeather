package com.neko.weather.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class AppTimeZoneTest {
    @Test
    fun resolvesBlankPreferenceToSystemZone() {
        assertEquals(ZoneId.systemDefault(), AppTimeZone.resolve(""))
    }

    @Test
    fun resolvesInvalidPreferenceToSystemZone() {
        assertEquals(ZoneId.systemDefault(), AppTimeZone.resolve("Not/AZone"))
    }

    @Test
    fun resolvesValidPreference() {
        assertEquals(ZoneId.of("Asia/Tokyo"), AppTimeZone.resolve("Asia/Tokyo"))
    }

    @Test
    fun locationZoneFallsBackToSystemForNullOrBlank() {
        assertEquals(ZoneId.systemDefault(), AppTimeZone.locationZone(null))
        assertEquals(ZoneId.systemDefault(), AppTimeZone.locationZone(" "))
    }

    @Test
    fun locationZoneAcceptsOffsetIds() {
        assertEquals(ZoneId.of("+08:00"), AppTimeZone.locationZone("+08:00"))
    }

    @Test
    fun convertLocationTimeShiftsAcrossZones() {
        val shanghai = ZoneId.of("Asia/Shanghai")
        val tokyo = ZoneId.of("Asia/Tokyo")
        assertEquals(
            "19:00",
            AppTimeZone.convertLocationTime("2026-10-24T18:00", shanghai, tokyo),
        )
    }

    @Test
    fun convertLocationTimeKeepsTailWhenSameZone() {
        val shanghai = ZoneId.of("Asia/Shanghai")
        assertEquals("18:00", AppTimeZone.convertLocationTime("2026-10-24T18:00", shanghai, shanghai))
    }

    @Test
    fun convertLocationTimeReturnsTailOnUnparseableInput() {
        assertEquals("abcde", AppTimeZone.convertLocationTime("abcde", ZoneId.of("UTC"), ZoneId.of("Asia/Tokyo")))
    }

    @Test
    fun formatUtcOffsetUsesFixedWidth() {
        assertEquals("UTC+08:00", formatUtcOffset(8 * 3600))
        assertEquals("UTC-05:30", formatUtcOffset(-(5 * 3600 + 1800)))
        assertEquals("UTC+00:00", formatUtcOffset(0))
    }

    @Test
    fun catalogKeepsRegionCityIdsOnlyAndExcludesSystemZone() {
        val catalog = AppTimeZone.catalog(now = Instant.parse("2026-10-24T00:00:00Z"), systemZoneId = "Asia/Shanghai")
        assertTrue(catalog.any { it.region == "Asia" })
        assertTrue(catalog.none { it.region !in setOf("Africa", "America", "Antarctica", "Arctic", "Asia", "Atlantic", "Australia", "Europe", "Indian", "Pacific") })
        assertTrue(catalog.all { section -> section.options.none { it.zoneId == "Asia/Shanghai" } })
        assertTrue(catalog.all { section -> section.options.all { it.zoneId.contains('/') } })
        // 组内按偏移升序
        val asia = catalog.first { it.region == "Asia" }
        assertEquals(asia.options.map { it.offsetSeconds }.sorted(), asia.options.map { it.offsetSeconds })
    }

    @Test
    fun filterSectionsMatchesCityRegionAndOffset() {
        val catalog = AppTimeZone.catalog(now = Instant.parse("2026-10-24T00:00:00Z"), systemZoneId = "")
        val byCity = AppTimeZone.filterSections(catalog, "Tokyo")
        assertTrue(byCity.flatMap { it.options }.any { it.zoneId == "Asia/Tokyo" })
        val byOffset = AppTimeZone.filterSections(catalog, "UTC+09:00")
        assertTrue(byOffset.flatMap { it.options }.isNotEmpty())
        assertTrue(AppTimeZone.filterSections(catalog, "zzzz").isEmpty())
        assertEquals(catalog, AppTimeZone.filterSections(catalog, "  "))
    }

    @Test
    fun convertLocationDateTimeKeepsFullFormat() {
        val shanghai = ZoneId.of("Asia/Shanghai")
        val tokyo = ZoneId.of("Asia/Tokyo")
        assertEquals(
            "2026-10-24T19:00",
            AppTimeZone.convertLocationDateTime("2026-10-24T18:00", shanghai, tokyo),
        )
    }

    @Test
    fun dayCrossingConversionWorks() {
        // 上海 23:30 → 洛杉矶（次日 08:30，夏令时 UTC-7）
        val shanghai = ZoneId.of("Asia/Shanghai")
        val losAngeles = ZoneId.of("America/Los_Angeles")
        val converted = AppTimeZone.convertLocationDateTime("2026-10-24T23:30", shanghai, losAngeles)
        assertEquals(LocalDateTime.of(2026, 10, 24, 8, 30).toString(), converted)
    }
}
