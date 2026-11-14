package com.neko.weather.notification

import com.neko.weather.model.AppLanguage
import com.neko.weather.model.DailyForecast
import com.neko.weather.model.WeatherDashboard
import com.neko.weather.model.WeatherSnapshot
import com.neko.weather.ui.localizedText
import org.junit.Assert.assertEquals
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

    private fun body(language: AppLanguage, time: String, condition: String) =
        WeatherNotificationPublisher.precipitationBody(language, time, condition) { localizedText(it, language) }

    @Test
    fun chinesePrecipitationBodyIsUnchanged() {
        assertEquals(
            "预计 23:00 前后阵雪，路面可能湿滑，注意保暖与出行安全。",
            body(AppLanguage.SIMPLIFIED_CHINESE, "23:00", "阵雪"),
        )
        assertEquals(
            "预计 08:00 前后小雨，外出请带伞。",
            body(AppLanguage.SIMPLIFIED_CHINESE, "08:00", "小雨"),
        )
    }

    @Test
    fun englishPrecipitationBodyHasNoChineseLeftovers() {
        listOf("小雨", "阵雪", "冰雹").forEach { condition ->
            val text = body(AppLanguage.ENGLISH, "23:00", condition)
            assertFalse("英文正文仍含汉字：$text", text.any { it.code in 0x4E00..0x9FFF })
            assertTrue(text, text.startsWith("Expect "))
        }
    }

    @Test
    fun japanesePrecipitationBodyUsesJapaneseFraming() {
        val text = body(AppLanguage.JAPANESE, "23:00", "阵雪")
        assertTrue(text, text.contains("頃に"))
        assertFalse("日文正文仍残留中文框架：$text", text.contains("预计"))
        assertFalse(text, text.contains("前后"))
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

    /** 生产默认数据形态：Open-Meteo 的 past_days=1 让 daily[0] 是昨天，不能用下标当"今天"。 */
    @Test
    fun weeklyOverviewHandlesPastDaysData() {
        // 2026-11-15 是周日；daily[0] = 昨天(14日)，daily[1] = 今天(15日)，daily[2..8] = 下周一至周日
        val days = listOf(
            day("2026-11-14", 15.0, 25.0),                       // 昨天，晴，均温 20
            day("2026-11-15", -5.0, 5.0),                        // 今天，晴，均温 0
        ) + (16..19).map { day("2026-11-$it", 0.0, 10.0, condition = "小雨") } +
            (20..22).map { day("2026-11-$it", 0.0, 10.0, condition = "晴") }

        val text = WeatherNotificationPublisher.weeklyOverview(dashboard(days), LocalDate.of(2026, 11, 15))

        assertNotNull(text)
        // 下周一至周日是 4 雨 3 晴、均温 5，比今天(0)高 —— 应判为「小雨为主 + 上升」
        assertTrue(text!!, text.contains("小雨为主"))
        assertTrue(text, text.contains("上升"))
    }

    @Test
    fun weeklyOverviewIsNullWithoutForecast() {
        val text = WeatherNotificationPublisher.weeklyOverview(dashboard(emptyList()), LocalDate.of(2026, 10, 25))
        assertTrue(text == null)
    }
}
