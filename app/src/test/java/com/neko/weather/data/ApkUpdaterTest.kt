package com.neko.weather.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApkUpdaterTest {
    @Test
    fun parsesTotalFromContentRange() {
        assertEquals(10_000L, parseContentRangeTotal("bytes 1000-9999/10000"))
        assertEquals(2_048L, parseContentRangeTotal("bytes 0-1023/2048"))
        assertEquals(512L, parseContentRangeTotal("bytes 256-511/512"))
    }

    @Test
    fun rejectsUnknownContentRange() {
        assertNull(parseContentRangeTotal(null))
        assertNull(parseContentRangeTotal("bytes 0-99/*"))
        assertNull(parseContentRangeTotal("bytes 0-99/abc"))
        assertNull(parseContentRangeTotal("bytes 0-99/0"))
        assertNull(parseContentRangeTotal(""))
    }

    @Test
    fun progressStaysWithinBounds() {
        assertEquals(0.5f, progressFraction(50, 100)!!, 0.001f)
        assertEquals(0f, progressFraction(0, 100)!!, 0.001f)
        assertEquals(1f, progressFraction(120, 100)!!, 0.001f)
    }

    @Test
    fun progressIsIndeterminateWithoutTotal() {
        assertNull(progressFraction(10, null))
        assertNull(progressFraction(10, 0))
        assertNull(progressFraction(10, -1))
    }
}
