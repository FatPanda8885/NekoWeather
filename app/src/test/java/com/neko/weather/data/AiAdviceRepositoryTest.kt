package com.neko.weather.data

import com.neko.weather.model.AiSettings
import com.neko.weather.model.WeatherDashboard
import com.neko.weather.model.WeatherSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiAdviceRepositoryTest {
    @Test
    fun completesDeepSeekAnthropicEndpoint() {
        val repository = AiAdviceRepository()

        assertEquals("https://api.deepseek.com/anthropic/v1/messages", repository.resolveEndpoint("https://api.deepseek.com"))
        assertEquals("https://api.deepseek.com/anthropic/v1/messages", repository.resolveEndpoint("https://api.deepseek.com/anthropic/"))
        assertEquals("https://api.anthropic.com/v1/messages", repository.resolveEndpoint("https://api.anthropic.com/v1/messages"))
    }

    @Test
    fun replacesRawJsonPlaceholders() {
        val weather = WeatherDashboard(
            current = WeatherSnapshot("上海", 30.0, "晴朗", 60.0, 8.0),
        )
        val settings = AiSettings(model = "test-model", maxTokens = 512, maxOutputCharacters = 900, context = "中文上下文")

        val json = AiAdviceRepository().buildRequestJson(settings, weather)

        assertEquals("test-model", json.getString("model"))
        assertEquals(512, json.getInt("max_tokens"))
        assertEquals("中文上下文", json.getString("system"))
        assertTrue(json.getJSONArray("messages").getJSONObject(0).getString("content").contains("上海"))
        assertTrue(json.getJSONArray("messages").getJSONObject(0).getString("content").contains("不超过900个字符"))
    }
}
