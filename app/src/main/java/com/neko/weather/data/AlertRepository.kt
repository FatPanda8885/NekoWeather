package com.neko.weather.data

import com.neko.weather.model.WeatherAlert
import com.neko.weather.model.WeatherApiConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

data class AlertResult(
    val source: String?,
    val alerts: List<WeatherAlert>,
    val checked: Boolean,
    val message: String,
)

class AlertRepository(private val client: OkHttpClient) {
    private data class CacheEntry(val createdAt: Long, val result: AlertResult)

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    fun fetch(config: WeatherApiConfig): AlertResult {
        if (config.alertProvider == "OFF") return unsupported("气象预警数据源已关闭")
        val latitude = config.latitude.toDoubleOrNull()
            ?: return unsupported("位置纬度无效，无法查询预警")
        val longitude = config.longitude.toDoubleOrNull()
            ?: return unsupported("位置经度无效，无法查询预警")
        if (config.alertProvider == "QWEATHER") return fetchQWeather(config, latitude, longitude)
        if (config.alertProvider == "CUSTOM") return fetchCustom(config)
        val provider = providerFor(config.locationName, config.locationAdmin, config.locationCountry)
            ?: return unsupported("该地区尚未配置免注册的官方预警数据源")
        val key = "$provider:${"%.2f".format(Locale.US, latitude)}:${"%.2f".format(Locale.US, longitude)}"
        val cached = cache[key]
        if (cached != null && System.currentTimeMillis() - cached.createdAt < CACHE_MILLIS) {
            return cached.result
        }

        return runCatching {
            when (provider) {
                Provider.NWS -> fetchNws(latitude, longitude)
                Provider.DWD -> fetchDwd(latitude, longitude)
            }
        }.fold(
            onSuccess = { result ->
                cache[key] = CacheEntry(System.currentTimeMillis(), result)
                result
            },
            onFailure = { error ->
                cached?.result?.copy(message = "预警源暂时不可用，显示上次结果")
                    ?: AlertResult(
                        source = provider.source,
                        alerts = emptyList(),
                        checked = false,
                        message = "${provider.source} 查询失败：${error.message ?: "网络错误"}",
                    )
            },
        )
    }

    private fun fetchQWeather(config: WeatherApiConfig, latitude: Double, longitude: Double): AlertResult {
        if (config.qWeatherApiKey.isBlank()) return unsupported("请先填写和风天气 API Key")
        return runCatching {
            val host = config.qWeatherApiHost.trim().removePrefix("https://").removePrefix("http://").trimEnd('/')
            val url = "https://$host".toHttpUrl().newBuilder()
                .encodedPath("/weatheralert/v1/current/${"%.2f".format(Locale.US, latitude)}/${"%.2f".format(Locale.US, longitude)}")
                .setQueryParameter("localTime", "true").setQueryParameter("lang", "zh").build()
            val request = Request.Builder().url(url).header("X-QW-Api-Key", config.qWeatherApiKey)
                .header("Accept", "application/json").header("User-Agent", USER_AGENT).build()
            val root = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                JSONObject(response.body?.string().orEmpty())
            }
            val array = root.optJSONArray("alerts")
            val alerts = buildList {
                for (index in 0 until (array?.length() ?: 0)) {
                    val item = array?.optJSONObject(index) ?: continue
                    val title = item.optString("headline").ifBlank { item.optJSONObject("eventType")?.optString("name").orEmpty() }
                    if (title.isBlank()) continue
                    add(WeatherAlert(
                        title = title,
                        severity = severityLabel(item.optString("severity")),
                        description = listOf(item.optString("description"), item.optString("instruction")).filter(String::isNotBlank).joinToString("\n").ifBlank { "请关注官方后续信息。" },
                        source = item.optString("senderName").ifBlank { "和风天气气象预警" },
                        expiresAt = item.optString("expireTime").takeIf(String::isNotBlank),
                    ))
                }
            }
            AlertResult("和风天气", alerts, true, "已检查和风天气当前生效预警")
        }.getOrElse { AlertResult("和风天气", emptyList(), false, "和风天气预警查询失败：${it.message}") }
    }

    private fun fetchCustom(config: WeatherApiConfig): AlertResult {
        val url = config.alertEndpoint.toHttpUrlOrNull()
            ?: return unsupported("自定义预警 API 地址无效")
        return runCatching {
            val request = Request.Builder().url(url)
                .header("Accept", "application/geo+json")
                .header("User-Agent", USER_AGENT)
                .build()
            val alerts = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                parseNws(JSONObject(response.body?.string().orEmpty())).map { it.copy(source = "自定义预警 API") }
            }
            AlertResult("自定义预警 API", alerts, true, "已检查自定义 GeoJSON/CAP 预警源")
        }.getOrElse {
            AlertResult("自定义预警 API", emptyList(), false, "自定义预警 API 查询失败：${it.message}")
        }
    }

    internal fun parseNws(root: JSONObject): List<WeatherAlert> {
        val features = root.optJSONArray("features") ?: return emptyList()
        return buildList {
            for (index in 0 until features.length()) {
                val properties = features.optJSONObject(index)?.optJSONObject("properties") ?: continue
                val title = properties.optString("headline").ifBlank { properties.optString("event") }
                if (title.isBlank()) continue
                val description = listOf(
                    properties.optString("description"),
                    properties.optString("instruction"),
                ).filter(String::isNotBlank).distinct().joinToString("\n")
                add(
                    WeatherAlert(
                        title = title,
                        severity = severityLabel(properties.optString("severity")),
                        description = description.ifBlank { "请关注当地官方发布的后续信息。" },
                        source = "美国国家气象局 NWS",
                        expiresAt = properties.optString("expires").takeIf(String::isNotBlank),
                    ),
                )
            }
        }
    }

    private fun fetchNws(latitude: Double, longitude: Double): AlertResult {
        val url = "https://api.weather.gov/alerts/active".toHttpUrl().newBuilder()
            .setQueryParameter("point", "$latitude,$longitude")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/geo+json")
            .header("User-Agent", USER_AGENT)
            .build()
        val alerts = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            parseNws(JSONObject(response.body?.string().orEmpty()))
        }
        return AlertResult("美国国家气象局 NWS", alerts, true, "已检查 NWS 当前生效预警")
    }

    private fun fetchDwd(latitude: Double, longitude: Double): AlertResult {
        val directory = DWD_DIRECTORY.toHttpUrl()
        val listingRequest = Request.Builder().url(directory).header("User-Agent", USER_AGENT).build()
        val listing = client.newCall(listingRequest).execute().use { response ->
            if (!response.isSuccessful) throw IOException("目录 HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
        val filename = DWD_FILE_REGEX.findAll(listing)
            .map { it.groupValues[1] }
            .maxOrNull()
            ?: throw IOException("未找到 DWD CAP 数据文件")
        val archiveRequest = Request.Builder()
            .url(directory.resolve(filename) ?: throw IOException("DWD 文件地址无效"))
            .header("User-Agent", USER_AGENT)
            .build()
        val archive = client.newCall(archiveRequest).execute().use { response ->
            if (!response.isSuccessful) throw IOException("数据 HTTP ${response.code}")
            response.body?.bytes() ?: throw IOException("DWD 返回空数据")
        }
        if (archive.size > MAX_ARCHIVE_BYTES) throw IOException("DWD 数据文件过大")
        val alerts = parseDwdArchive(archive, latitude, longitude)
        return AlertResult("德国气象局 DWD Open Data", alerts, true, "已检查 DWD 当前生效预警")
    }

    private fun parseDwdArchive(bytes: ByteArray, latitude: Double, longitude: Double): List<WeatherAlert> {
        val alerts = mutableListOf<WeatherAlert>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".xml", ignoreCase = true)) {
                    parseDwdCap(zip.readBytesLimited(MAX_ENTRY_BYTES), latitude, longitude)?.let(alerts::add)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return alerts.distinctBy { it.title to it.expiresAt }
    }

    private fun parseDwdCap(xml: ByteArray, latitude: Double, longitude: Double): WeatherAlert? {
        val parser = XmlPullParserFactory.newInstance().newPullParser().apply {
            setInput(ByteArrayInputStream(xml), "UTF-8")
        }
        var language = ""
        var severity = ""
        var headline = ""
        var description = ""
        var instruction = ""
        var expires = ""
        val polygons = mutableListOf<String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name.substringAfter(':')) {
                    "language" -> language = parser.nextText()
                    "severity" -> severity = parser.nextText()
                    "headline" -> headline = parser.nextText()
                    "description" -> description = parser.nextText()
                    "instruction" -> instruction = parser.nextText()
                    "expires" -> expires = parser.nextText()
                    "polygon" -> polygons += parser.nextText()
                }
            }
            event = parser.next()
        }
        if (!language.startsWith("de", ignoreCase = true)) return null
        if (headline.isBlank() || polygons.none { containsPoint(it, latitude, longitude) }) return null
        return WeatherAlert(
            title = headline,
            severity = severityLabel(severity),
            description = listOf(description, instruction).filter(String::isNotBlank).distinct().joinToString("\n"),
            source = "德国气象局 DWD Open Data",
            expiresAt = expires.takeIf(String::isNotBlank),
        )
    }

    internal fun containsPoint(polygon: String, latitude: Double, longitude: Double): Boolean {
        val points = polygon.trim().split(Regex("\\s+")).mapNotNull { pair ->
            val values = pair.split(',')
            if (values.size != 2) null else {
                val lat = values[0].toDoubleOrNull()
                val lon = values[1].toDoubleOrNull()
                if (lat == null || lon == null) null else lat to lon
            }
        }
        if (points.size < 3) return false
        var inside = false
        var previous = points.last()
        for (current in points) {
            val crosses = (current.first > latitude) != (previous.first > latitude) &&
                longitude < (previous.second - current.second) * (latitude - current.first) /
                (previous.first - current.first) + current.second
            if (crosses) inside = !inside
            previous = current
        }
        return inside
    }

    private fun ByteArrayInputStream.readBytesLimited(limit: Int): ByteArray = readBytes().also {
        if (it.size > limit) throw IOException("CAP 条目过大")
    }

    private fun ZipInputStream.readBytesLimited(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            if (total > limit) throw IOException("CAP 条目过大")
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun severityLabel(value: String): String = when (value.lowercase(Locale.ROOT)) {
        "extreme" -> "极端"
        "severe" -> "严重"
        "moderate" -> "中等"
        "minor" -> "较低"
        else -> "级别未知"
    }

    private fun providerFor(locationName: String, locationAdmin: String, locationCountry: String): Provider? {
        val hint = "$locationName $locationAdmin $locationCountry".lowercase(Locale.ROOT)
        return when {
            listOf("美国", "united states", "usa", "u.s.").any(hint::contains) -> Provider.NWS
            listOf("德国", "germany", "deutschland").any(hint::contains) -> Provider.DWD
            else -> null
        }
    }

    private fun unsupported(message: String) = AlertResult(null, emptyList(), false, message)

    private enum class Provider(val source: String) {
        NWS("美国国家气象局 NWS"),
        DWD("德国气象局 DWD Open Data"),
    }

    private companion object {
        const val USER_AGENT = "NekoWeather/0.1 (Android weather client)"
        const val DWD_DIRECTORY = "https://opendata.dwd.de/weather/alerts/cap/COMMUNEUNION_DWD_STAT/"
        val DWD_FILE_REGEX = Regex("href=\\\"([^\\\"]*STATUS_PREMIUMDWD_COMMUNEUNION_DE\\.zip)\\\"", RegexOption.IGNORE_CASE)
        const val CACHE_MILLIS = 5 * 60 * 1000L
        const val MAX_ARCHIVE_BYTES = 8 * 1024 * 1024
        const val MAX_ENTRY_BYTES = 1024 * 1024
    }
}
