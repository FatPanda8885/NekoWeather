package com.neko.weather.notification

import com.neko.weather.model.DailyForecast
import com.neko.weather.model.WeatherDashboard
import com.neko.weather.model.WeatherSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeatherNotificationContentTest {
    @Test
    fun treatsEveryPrecipitationKindAsWet() {
        listOf("小雨", "中雨", "阵雨", "冻雨", "雷阵雨", "下雨", "毛毛雨", "下雪", "阵雪", "雨夹雪", "冰雹", "雷暴")
            .forEach { assertTrue("$it 应判定为降水", WeatherNotificationPublisher.isPrecipitation(it)) }
        listOf("晴朗", "多云", "阴天", "有雾", "霾", "天气未知")
            .forEach { assertFalse("$it 不应判定为降水", WeatherNotificationPublisher.isPrecipitation(it)) }
    }

    private fun day(date: String, low: Double, high: Double, condition: String = "多云") =
        DailyForecast(date, high, low, condition, null, null, null, null, null, null)

    private fun dashboard(days: List<DailyForecast>) = WeatherDashboard(
        current = WeatherSnapshot("上海", 20.0, "多云", 60.0, 8.0),
        daily = days,
    )

    @Test
    fun weeklyOverviewReportsDominantWeatherAndWarming() {
        // 2026-10-22 是周四：本周剩余 = 22/23/24/25 共 4 天，均温 15
        val days = listOf(
            day("2026-10-22", 10.0, 20.0),
            day("2026-10-23", 10.0, 20.0),
            day("2026-10-24", 10.0, 20.0),
            day("2026-10-25", 10.0, 20.0),
        ) + (26..32).map { day("2026-10-$it", 20.0, 30.0, condition = if (it % 2 == 0) "小雨" else "小雨") }

        val text = WeatherNotificationPublisher.weeklyOverview(dashboard(days), LocalDate.of(2026, 10, 22))

        assertNotNull(text)
        assertTrue(text!!, text.contains("小雨为主"))
        assertTrue(text, text.contains("上升"))
    }

    @Test
    fun weeklyOverviewFallsBackToTodayWhenTheWeekHasNoRoom() {
        // 2026-10-25 是周日：本周剩余只剩今天，退回与今天比较
        val days = listOf(day("2026-10-25", 10.0, 20.0)) +
            (26..32).map { day("2026-10-$it", 0.0, 10.0, condition = "阵雪") }

        val text = WeatherNotificationPublisher.weeklyOverview(dashboard(days), LocalDate.of(2026, 10, 25))

        assertNotNull(text)
        assertTrue(text!!, text.contains("阵雪为主"))
        assertTrue(text, text.contains("下降"))
    }

    @Test
    fun weeklyOverviewIsNullWithoutForecast() {
        val text = WeatherNotificationPublisher.weeklyOverview(dashboard(emptyList()), LocalDate.of(2026, 10, 25))
        assertTrue(text == null)
    }
}
