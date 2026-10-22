package com.neko.weather.data

import com.neko.weather.model.AiModelProfile
import com.neko.weather.model.AiProtocol
import com.neko.weather.model.WeatherDashboard
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AiAdviceRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build(),
) {
    fun request(profile: AiModelProfile, weather: WeatherDashboard): String {
        return client.newCall(buildRequest(profile, weather)).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching { JSONObject(raw).optJSONObject("error")?.optString("message") }.getOrNull()
                val fallback = if (response.code == 404) {
                    "AI API 返回 HTTP 404，请确认服务支持 ${profile.protocol.label}；请求地址：${response.request.url}"
                } else {
                    "AI API 返回 HTTP ${response.code}"
                }
                throw IOException(detail?.takeIf(String::isNotBlank) ?: fallback)
            }
            parseResponse(JSONObject(raw), profile.protocol)
                .take(profile.maxOutputCharacters.coerceIn(100, 10000))
        }
    }

    internal fun buildRequest(profile: AiModelProfile, weather: WeatherDashboard): Request {
        val endpoint = resolveEndpoint(profile.endpoint, profile.protocol)
        require(endpoint.startsWith("https://")) { "AI API 地址必须使用 HTTPS" }
        require(profile.apiKey.isNotBlank()) { "请先配置 AI API Key" }
        val body = buildRequestJson(profile, weather).toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        return Request.Builder()
            .url(endpoint)
            .header("content-type", "application/json")
            .header("accept", "application/json")
            .apply {
                when (profile.protocol) {
                    AiProtocol.ANTHROPIC_MESSAGES -> {
                        header("x-api-key", profile.apiKey)
                        header("authorization", "Bearer ${profile.apiKey}")
                        header("anthropic-version", "2023-06-01")
                    }
                    AiProtocol.OPENAI_CHAT -> header("authorization", "Bearer ${profile.apiKey}")
                }
            }
            .post(body)
            .build()
    }

    internal fun resolveEndpoint(raw: String, protocol: AiProtocol): String {
        if (raw.isBlank()) return protocol.defaultEndpoint
        return when (protocol) {
            AiProtocol.ANTHROPIC_MESSAGES -> resolveAnthropicEndpoint(raw)
            AiProtocol.OPENAI_CHAT -> resolveOpenAiEndpoint(raw)
        }
    }

    private fun resolveAnthropicEndpoint(raw: String): String {
        val endpoint = raw.trim().trimEnd('/')
        val url = endpoint.toHttpUrlOrNull() ?: return endpoint
        val path = url.encodedPath.trimEnd('/').ifBlank { "/" }
        val targetPath = when {
            url.host == "api.deepseek.com" && path in setOf("/", "/anthropic", "/v1/messages") -> "/anthropic/v1/messages"
            path == "/anthropic" -> "/anthropic/v1/messages"
            path == "/v1" -> "/v1/messages"
            else -> return endpoint
        }
        return url.newBuilder().encodedPath(targetPath).build().toString()
    }

    private fun resolveOpenAiEndpoint(raw: String): String {
        val endpoint = raw.trim().trimEnd('/')
        val url = endpoint.toHttpUrlOrNull() ?: return endpoint
        val path = url.encodedPath.trimEnd('/').ifBlank { "/" }
        val targetPath = when {
            path == "/" -> "/v1/chat/completions"
            VERSION_PATH.matches(path) -> "$path/chat/completions"
            else -> return endpoint
        }
        return url.newBuilder().encodedPath(targetPath).build().toString()
    }

    internal fun buildRequestJson(profile: AiModelProfile, weather: WeatherDashboard): JSONObject {
        val maxCharacters = profile.maxOutputCharacters.coerceIn(100, 10000)
        val prompt = weatherPayload(weather).toString(2) +
            "\n请基于以上天气数据给出今日生活、出行和风险建议。使用简洁 Markdown 排版，最终输出不超过${maxCharacters}个字符。"
        val rendered = profile.rawRequestJson
            .replace("{{model_json}}", JSONObject.quote(profile.model))
            .replace("{{max_tokens}}", profile.maxTokens.coerceIn(64, 8192).toString())
            .replace("{{context_json}}", JSONObject.quote(profile.context))
            .replace("{{weather_json}}", JSONObject.quote(prompt))
        return try {
            JSONObject(rendered)
        } catch (error: Exception) {
            throw IllegalArgumentException("原始请求 JSON 无效：${error.message}")
        }
    }

    internal fun parseResponse(root: JSONObject, protocol: AiProtocol): String = when (protocol) {
        AiProtocol.ANTHROPIC_MESSAGES -> parseAnthropicResponse(root)
        AiProtocol.OPENAI_CHAT -> parseOpenAiResponse(root)
    }

    private fun parseAnthropicResponse(root: JSONObject): String {
        val content = root.optJSONArray("content") ?: throw IOException("AI 响应缺少 content 数组")
        val text = buildList {
            for (index in 0 until content.length()) {
                val item = content.optJSONObject(index) ?: continue
                if (item.optString("type") == "text") item.optString("text").takeIf(String::isNotBlank)?.let(::add)
            }
        }.joinToString("\n")
        if (text.isBlank()) throw IOException("AI 响应中没有文本内容")
        return text
    }

    private fun parseOpenAiResponse(root: JSONObject): String {
        val choices = root.optJSONArray("choices") ?: throw IOException("AI 响应缺少 choices 内容")
        val text = (0 until choices.length())
            .firstNotNullOfOrNull { index ->
                choices.optJSONObject(index)?.optJSONObject("message")
                    ?.optString("content")?.takeIf(String::isNotBlank)
            }
        return text ?: throw IOException("AI 响应缺少 choices 内容")
    }

    private fun weatherPayload(weather: WeatherDashboard): JSONObject = JSONObject().apply {
        put("location", weather.current.location)
        put("current", JSONObject().apply {
            put("condition", weather.current.condition)
            put("temperature_c", weather.current.temperature)
            put("apparent_temperature_c", weather.current.apparentTemperature)
            put("humidity_percent", weather.current.humidity)
            put("wind_kmh", weather.current.windSpeed)
            put("pressure_hpa", weather.current.pressure)
            put("visibility_km", weather.current.visibility)
        })
        put("next_12_hours", JSONArray(weather.hourly.take(12).map { hour -> JSONObject().apply {
            put("time", hour.time)
            put("condition", hour.condition)
            put("temperature_c", hour.temperature)
            put("rain_probability_percent", hour.precipitationProbability)
            put("precipitation_mm", hour.precipitationAmount)
        } }))
        put("next_7_days", JSONArray(weather.daily.drop(1).take(7).map { day -> JSONObject().apply {
            put("date", day.date)
            put("condition", day.condition)
            put("low_c", day.low)
            put("high_c", day.high)
            put("rain_probability_percent", day.precipitationProbability)
            put("uv_index", day.uvIndex)
        } }))
        weather.airQuality?.let { put("air_quality", JSONObject().put("standard", it.standard).put("aqi", it.aqi).put("pm2_5", it.pm25)) }
        put("active_alerts", JSONArray(weather.alerts.map { JSONObject().put("severity", it.severity).put("title", it.title).put("description", it.description) }))
    }

    private companion object {
        val VERSION_PATH = Regex("/v\\d+")
    }
}
