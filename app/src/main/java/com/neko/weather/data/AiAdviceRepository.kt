package com.neko.weather.data

import com.neko.weather.model.AiSettings
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
    fun request(settings: AiSettings, weather: WeatherDashboard): String {
        require(settings.endpoint.startsWith("https://")) { "AI API 地址必须使用 HTTPS" }
        require(settings.apiKey.isNotBlank()) { "请先配置 AI API Key" }
        val endpoint = resolveEndpoint(settings.endpoint)
        val bodyJson = buildRequestJson(settings, weather)
        val request = Request.Builder()
            .url(endpoint)
            .header("content-type", "application/json")
            .header("accept", "application/json")
            .header("x-api-key", settings.apiKey)
            .header("authorization", "Bearer ${settings.apiKey}")
            .header("anthropic-version", "2023-06-01")
            .post(bodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        return client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val detail = runCatching { JSONObject(raw).optJSONObject("error")?.optString("message") }.getOrNull()
                val fallback = if (response.code == 404) {
                    "AI API 返回 HTTP 404，请确认服务支持 Anthropic Messages；请求地址：$endpoint"
                } else {
                    "AI API 返回 HTTP ${response.code}"
                }
                throw IOException(detail?.takeIf(String::isNotBlank) ?: fallback)
            }
            parseResponse(JSONObject(raw)).take(settings.maxOutputCharacters.coerceIn(100, 10000))
        }
    }

    internal fun resolveEndpoint(raw: String): String {
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

    internal fun buildRequestJson(settings: AiSettings, weather: WeatherDashboard): JSONObject {
        val maxCharacters = settings.maxOutputCharacters.coerceIn(100, 10000)
        val prompt = weatherPayload(weather).toString(2) +
            "\n请基于以上天气数据给出今日生活、出行和风险建议。使用简洁 Markdown 排版，最终输出不超过${maxCharacters}个字符。"
        val rendered = settings.rawRequestJson
            .replace("{{model_json}}", JSONObject.quote(settings.model))
            .replace("{{max_tokens}}", settings.maxTokens.coerceIn(64, 8192).toString())
            .replace("{{context_json}}", JSONObject.quote(settings.context))
            .replace("{{weather_json}}", JSONObject.quote(prompt))
        return try {
            JSONObject(rendered)
        } catch (error: Exception) {
            throw IllegalArgumentException("原始请求 JSON 无效：${error.message}")
        }
    }

    private fun parseResponse(root: JSONObject): String {
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
}
