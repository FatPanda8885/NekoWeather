package com.neko.weather.data

import com.neko.weather.model.CityLocation
import com.neko.weather.model.WeatherApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class CityRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(ApiUsageInterceptor())
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun search(query: String, config: WeatherApiConfig): List<CityLocation> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        if (config.qWeatherApiKey.isNotBlank()) {
            runCatching { searchQWeather(query, config) }.getOrNull()?.takeIf(List<CityLocation>::isNotEmpty)?.let { return@withContext it }
        }
        searchOpenMeteo(query)
    }

    private fun searchQWeather(query: String, config: WeatherApiConfig): List<CityLocation> {
        val host = config.qWeatherApiHost.trim().removePrefix("https://").removePrefix("http://").trimEnd('/')
        val url = "https://$host/geo/v2/city/lookup".toHttpUrl().newBuilder()
            .setQueryParameter("location", query.trim()).setQueryParameter("number", "20").setQueryParameter("lang", "zh").build()
        val request = Request.Builder().url(url).header("X-QW-Api-Key", config.qWeatherApiKey).header("User-Agent", "NekoWeather/0.2").build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("和风 GeoAPI 返回 HTTP ${response.code}")
            val array = JSONObject(response.body?.string().orEmpty()).optJSONArray("location") ?: return@use emptyList()
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val latitude = item.optString("lat").toDoubleOrNull() ?: continue
                    val longitude = item.optString("lon").toDoubleOrNull() ?: continue
                    add(CityLocation(
                        id = "qweather-${item.optString("id")}", name = item.optString("name", "未知地点"),
                        admin = listOf(item.optString("adm2"), item.optString("adm1")).filter(String::isNotBlank).distinct().joinToString(" · "),
                        country = item.optString("country"), latitude = latitude, longitude = longitude,
                    ))
                }
            }
        }
    }

    private fun searchOpenMeteo(query: String): List<CityLocation> {
        val url = "https://geocoding-api.open-meteo.com/v1/search".toHttpUrl().newBuilder()
            .setQueryParameter("name", query.trim())
            .setQueryParameter("count", "20")
            .setQueryParameter("language", "zh")
            .setQueryParameter("format", "json")
            .build()
        val request = Request.Builder().url(url).header("User-Agent", "NekoWeather/0.1").build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("城市搜索失败：HTTP ${response.code}")
            val root = JSONObject(response.body?.string().orEmpty())
            val results = root.optJSONArray("results") ?: return@use emptyList()
            buildList {
                for (index in 0 until results.length()) {
                    val item = results.optJSONObject(index) ?: continue
                    val latitude = item.optDouble("latitude", Double.NaN)
                    val longitude = item.optDouble("longitude", Double.NaN)
                    if (!latitude.isFinite() || !longitude.isFinite()) continue
                    add(
                        CityLocation(
                            id = item.optLong("id", 0L).takeIf { it != 0L }?.toString()
                                ?: "geo-$latitude-$longitude",
                            name = item.optString("name", "未知地点"),
                            admin = item.optString("admin1"),
                            country = item.optString("country"),
                            latitude = latitude,
                            longitude = longitude,
                        ),
                    )
                }
            }
        }
    }
}
