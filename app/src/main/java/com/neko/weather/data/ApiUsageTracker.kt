package com.neko.weather.data

import android.content.Context
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

data class ApiQuota(
    val dailyEnabled: Boolean = false,
    val weeklyEnabled: Boolean = false,
    val monthlyEnabled: Boolean = false,
    val daily: Int = 1000,
    val weekly: Int = 5000,
    val monthly: Int = 20000,
) {
    val enabled: Boolean get() = dailyEnabled || weeklyEnabled || monthlyEnabled
}
data class ApiUsageSummary(val key: String, val provider: String, val url: String, val category: String, val daily: Int, val weekly: Int, val monthly: Int, val quota: ApiQuota)

object ApiUsageTracker {
    @Volatile private var context: Context? = null
    fun initialize(context: Context) { this.context = context.applicationContext }

    @Synchronized fun registerProvider(url: String, name: String) {
        val ctx = context ?: return
        if (name.isBlank()) return
        val host = runCatching { url.toHttpUrl().host }.getOrNull() ?: return
        val prefs = ctx.getSharedPreferences("api_usage", Context.MODE_PRIVATE)
        val names = JSONObject(prefs.getString("providerNames", "{}") ?: "{}")
        names.put(host, name)
        prefs.edit().putString("providerNames", names.toString()).apply()
    }

    fun keyFor(url: String): Triple<String, String, String> {
        val parsed = url.toHttpUrl()
        val hint = (parsed.host + parsed.encodedPath).lowercase()
        val category = when {
            listOf("staticmap", "staticimage", "coordinate/convert", "tile.openstreetmap", "mapservice/gettile").any(hint::contains) -> "地图底图"
            hint.contains("rainviewer") -> "降雨云图"
            listOf("air-quality", "airquality", "/air/").any(hint::contains) -> "空气质量"
            listOf("warning", "weatheralert", "alerts", "nws", "dwd").any(hint::contains) -> "气象预警"
            else -> "天气"
        }
        return Triple("$category|${parsed.host}", parsed.host, category)
    }

    @Synchronized fun checkAndRecord(url: String) {
        val ctx = context ?: return
        val (key, _, _) = keyFor(url)
        val summaries = summaries().associateBy { it.key }
        val current = summaries[key]
        current?.quota?.takeIf { it.enabled }?.let { quota ->
            if ((quota.dailyEnabled && current.daily >= quota.daily) ||
                (quota.weeklyEnabled && current.weekly >= quota.weekly) ||
                (quota.monthlyEnabled && current.monthly >= quota.monthly)
            ) {
                throw IOException("供应商请求次数已达到限制：$key")
            }
        }
        val prefs = ctx.getSharedPreferences("api_usage", Context.MODE_PRIVATE)
        val events = JSONArray(prefs.getString("events", "[]"))
        val safeUrl = url.toHttpUrl().newBuilder().query(null).fragment(null).build().toString()
        events.put(JSONObject().put("key", key).put("url", safeUrl).put("time", System.currentTimeMillis()))
        val cutoff = System.currentTimeMillis() - 40L * 24 * 60 * 60 * 1000
        val trimmed = JSONArray()
        for (i in 0 until events.length()) {
            val time = events.optJSONObject(i)?.optLong("time", 0L) ?: 0L
            if (time >= cutoff) trimmed.put(events.get(i))
        }
        prefs.edit().putString("events", trimmed.toString()).apply()
    }

    @Synchronized fun summaries(): List<ApiUsageSummary> {
        val ctx = context ?: return emptyList()
        val prefs = ctx.getSharedPreferences("api_usage", Context.MODE_PRIVATE)
        val events = JSONArray(prefs.getString("events", "[]"))
        val quotas = JSONObject(prefs.getString("quotas", "{}") ?: "{}")
        val providerNames = JSONObject(prefs.getString("providerNames", "{}") ?: "{}")
        val now = java.time.ZonedDateTime.now()
        val counts = linkedMapOf<String, IntArray>()
        val urls = linkedMapOf<String, String>()
        for (i in 0 until events.length()) {
            val event = events.optJSONObject(i) ?: continue
            val key = event.optString("key")
            event.optString("url").takeIf(String::isNotBlank)?.let { urls[key] = it }
            val time = Instant.ofEpochMilli(event.optLong("time")).atZone(ZoneId.systemDefault())
            val values = counts.getOrPut(key) { IntArray(3) }
            if (time.toLocalDate() == now.toLocalDate()) values[0]++
            val wf = WeekFields.of(Locale.getDefault())
            if (time.get(wf.weekOfWeekBasedYear()) == now.get(wf.weekOfWeekBasedYear()) && time.year == now.year) values[1]++
            if (time.year == now.year && time.month == now.month) values[2]++
        }
        val allKeys = (counts.keys + quotas.keys().asSequence().toList()).distinct()
        return allKeys.map { key ->
            val parts = key.split('|', limit = 2)
            val q = quotas.optJSONObject(key)
            val legacyEnabled = q?.optBoolean("enabled") ?: false
            val quota = ApiQuota(
                dailyEnabled = q?.optBoolean("dailyEnabled", legacyEnabled) ?: false,
                weeklyEnabled = q?.optBoolean("weeklyEnabled", legacyEnabled) ?: false,
                monthlyEnabled = q?.optBoolean("monthlyEnabled", legacyEnabled) ?: false,
                daily = q?.optInt("daily", 1000) ?: 1000,
                weekly = q?.optInt("weekly", 5000) ?: 5000,
                monthly = q?.optInt("monthly", 20000) ?: 20000,
            )
            val c = counts[key] ?: IntArray(3)
            val host = parts.getOrElse(1) { key }
            val provider = providerNames.optString(host).takeIf(String::isNotBlank) ?: when {
                host.contains("open-meteo.com") -> "Open-Meteo"
                host.contains("qweather.com") -> "和风天气"
                host.contains("amap.com") -> "高德地图"
                host.contains("baidu.com") -> "百度地图"
                host.contains("huawei.com") -> "花瓣地图"
                host.contains("openstreetmap.org") -> "OpenStreetMap"
                host.contains("rainviewer.com") -> "RainViewer"
                host.contains("nmc.cn") -> "中央气象台"
                host.contains("weather.gov") -> "美国国家气象局"
                host.contains("dwd.de") -> "德国气象局"
                else -> host.removePrefix("www.")
            }
            ApiUsageSummary(key, provider, urls[key] ?: "https://$host", parts.firstOrNull() ?: "天气", c[0], c[1], c[2], quota)
        }
    }

    @Synchronized fun saveQuota(key: String, quota: ApiQuota) {
        val ctx = context ?: return
        val prefs = ctx.getSharedPreferences("api_usage", Context.MODE_PRIVATE)
        val root = JSONObject(prefs.getString("quotas", "{}") ?: "{}")
        root.put(key, JSONObject()
            .put("dailyEnabled", quota.dailyEnabled)
            .put("weeklyEnabled", quota.weeklyEnabled)
            .put("monthlyEnabled", quota.monthlyEnabled)
            .put("daily", quota.daily).put("weekly", quota.weekly).put("monthly", quota.monthly))
        prefs.edit().putString("quotas", root.toString()).apply()
    }
}

class ApiUsageInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        ApiUsageTracker.checkAndRecord(chain.request().url.toString())
        return chain.proceed(chain.request())
    }
}
