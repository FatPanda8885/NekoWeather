package com.neko.weather.data

import com.neko.weather.model.AiModelProfile
import com.neko.weather.model.AiProtocol
import com.neko.weather.model.WeatherDashboard
import com.neko.weather.model.WeatherSnapshot
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AiAdviceRepositoryTest {
    private val weather = WeatherDashboard(
        current = WeatherSnapshot("上海", 30.0, "晴朗", 60.0, 8.0),
    )

    @Test
    fun completesDeepSeekAnthropicEndpoint() {
        val repository = AiAdviceRepository()
        val protocol = AiProtocol.ANTHROPIC_MESSAGES

        assertEquals("https://api.deepseek.com/anthropic/v1/messages", repository.resolveEndpoint("https://api.deepseek.com", protocol))
        assertEquals("https://api.deepseek.com/anthropic/v1/messages", repository.resolveEndpoint("https://api.deepseek.com/anthropic/", protocol))
        assertEquals("https://api.anthropic.com/v1/messages", repository.resolveEndpoint("https://api.anthropic.com/v1/messages", protocol))
    }

    @Test
    fun completesOpenAiEndpoint() {
        val repository = AiAdviceRepository()
        val protocol = AiProtocol.OPENAI_CHAT

        assertEquals("https://api.openai.com/v1/chat/completions", repository.resolveEndpoint("https://api.openai.com", protocol))
        assertEquals("https://api.deepseek.com/v1/chat/completions", repository.resolveEndpoint("https://api.deepseek.com", protocol))
        assertEquals("https://api.openai.com/v1/chat/completions", repository.resolveEndpoint("https://api.openai.com/v1", protocol))
        assertEquals("https://api.openai.com/v1/chat/completions", repository.resolveEndpoint("https://api.openai.com/v1/chat/completions", protocol))
        assertEquals("https://gateway.example.com/proxy/chat", repository.resolveEndpoint("https://gateway.example.com/proxy/chat", protocol))
        assertEquals(AiProtocol.OPENAI_CHAT.defaultEndpoint, repository.resolveEndpoint("   ", protocol))
    }

    @Test
    fun replacesRawJsonPlaceholders() {
        val profile = AiModelProfile(
            id = "",
            name = "测试",
            protocol = AiProtocol.ANTHROPIC_MESSAGES,
            model = "test-model",
            maxTokens = 512,
            maxOutputCharacters = 900,
            context = "中文上下文",
        )

        val json = AiAdviceRepository().buildRequestJson(profile, weather)

        assertEquals("test-model", json.getString("model"))
        assertEquals(512, json.getInt("max_tokens"))
        assertEquals("中文上下文", json.getString("system"))
        val content = json.getJSONArray("messages").getJSONObject(0).getString("content")
        assertTrue(content.contains("上海"))
        assertTrue(content.contains("不超过900个字符"))
    }

    @Test
    fun buildsOpenAiCompatibleBody() {
        val profile = AiModelProfile(
            id = "",
            name = "OpenAI",
            protocol = AiProtocol.OPENAI_CHAT,
            model = "gpt-4o-mini",
            maxTokens = 640,
            maxOutputCharacters = 800,
            context = "中文上下文",
        )

        val json = AiAdviceRepository().buildRequestJson(profile, weather)

        assertEquals("gpt-4o-mini", json.getString("model"))
        assertEquals(640, json.getInt("max_tokens"))
        assertEquals("中文上下文", json.getJSONArray("messages").getJSONObject(0).getString("content"))
        assertEquals("user", json.getJSONArray("messages").getJSONObject(1).getString("role"))
        assertTrue(json.getJSONArray("messages").getJSONObject(1).getString("content").contains("上海"))
    }

    @Test
    fun sendsAnthropicHeadersOnlyForAnthropicProtocol() {
        val repository = AiAdviceRepository()

        val anthropic = repository.buildRequest(AiModelProfile(id = "", name = "a", apiKey = "secret"), weather)
        assertEquals("secret", anthropic.header("x-api-key"))
        assertEquals("Bearer secret", anthropic.header("authorization"))
        assertEquals("2023-06-01", anthropic.header("anthropic-version"))

        val openAi = repository.buildRequest(
            AiModelProfile(id = "", name = "b", protocol = AiProtocol.OPENAI_CHAT, apiKey = "secret"),
            weather,
        )
        assertNull(openAi.header("x-api-key"))
        assertNull(openAi.header("anthropic-version"))
        assertEquals("Bearer secret", openAi.header("authorization"))
        assertEquals("https://api.openai.com/v1/chat/completions", openAi.url.toString())
    }

    @Test
    fun parsesAnthropicResponse() {
        val root = JSONObject("""{"content":[{"type":"text","text":"出门带伞"}]}""")

        assertEquals("出门带伞", AiAdviceRepository().parseResponse(root, AiProtocol.ANTHROPIC_MESSAGES))
    }

    @Test
    fun parsesOpenAiResponse() {
        val root = JSONObject("""{"choices":[{"message":{"role":"assistant","content":"带伞更稳妥"}}]}""")

        assertEquals("带伞更稳妥", AiAdviceRepository().parseResponse(root, AiProtocol.OPENAI_CHAT))
    }

    @Test
    fun reportsMissingOpenAiChoices() {
        val root = JSONObject("""{"error":{"message":"bad"}}""")

        val error = runCatching { AiAdviceRepository().parseResponse(root, AiProtocol.OPENAI_CHAT) }.exceptionOrNull()

        assertTrue(error is IOException)
        assertEquals("AI 响应缺少 choices 内容", error?.message)
    }
}
