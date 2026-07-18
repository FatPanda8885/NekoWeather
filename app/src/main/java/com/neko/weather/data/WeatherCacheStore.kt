package com.neko.weather.data

import android.content.Context
import com.neko.weather.model.AirQuality
import com.neko.weather.model.DailyForecast
import com.neko.weather.model.HourlyForecast
import com.neko.weather.model.SolarPosition
import com.neko.weather.model.WeatherAlert
import com.neko.weather.model.WeatherApiConfig
import com.neko.weather.model.WeatherDashboard
import com.neko.weather.model.WeatherSnapshot
import org.json.JSONArray
import org.json.JSONObject

/** Small last-response cache used to enforce refresh freshness across process restarts. */
class WeatherCacheStore(context: Context) {
    private val preferences = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)

    fun load(config: WeatherApiConfig): WeatherDashboard? = runCatching {
        if (preferences.getString("fingerprint", "") != fingerprint(config)) return null
        val raw = preferences.getString("dashboard", null) ?: return null
        decode(JSONObject(raw))
    }.getOrNull()

    fun save(config: WeatherApiConfig, dashboard: WeatherDashboard) {
        preferences.edit()
            .putString("fingerprint", fingerprint(config))
            .putString("dashboard", encode(dashboard).toString())
            .apply()
    }

    fun clear() = preferences.edit().clear().apply()

    private fun fingerprint(config: WeatherApiConfig) = listOf(
        config.endpoint, config.latitude, config.longitude, config.displayName,
        config.airQualityEndpoint, config.alertProvider, config.alertEndpoint,
        config.qWeatherApiHost,
    ).joinToString("\u001f")

    private fun encode(value: WeatherDashboard): JSONObject = JSONObject().apply {
        put("current", JSONObject().apply {
            put("location", value.current.location); put("temperature", value.current.temperature)
            put("condition", value.current.condition); putNullable("humidity", value.current.humidity)
            putNullable("windSpeed", value.current.windSpeed); putNullable("apparentTemperature", value.current.apparentTemperature)
            putNullable("windDirection", value.current.windDirection); putNullable("pressure", value.current.pressure)
            putNullable("visibility", value.current.visibility); putNullable("observationTime", value.current.observationTime)
            putNullable("solarElevation", value.current.solarElevation); putNullable("timezone", value.current.timezone)
            put("fetchedAtMillis", value.current.fetchedAtMillis)
        })
        put("hourly", array(value.hourly) { item -> JSONObject().apply {
            put("time", item.time); put("temperature", item.temperature); put("condition", item.condition)
            putNullable("precipitationProbability", item.precipitationProbability)
            putNullable("precipitationAmount", item.precipitationAmount)
            putNullable("windSpeed", item.windSpeed); putNullable("windDirection", item.windDirection)
        } })
        put("daily", array(value.daily) { item -> JSONObject().apply {
            put("date", item.date); put("high", item.high); put("low", item.low); put("condition", item.condition)
            putNullable("precipitationProbability", item.precipitationProbability); putNullable("windSpeed", item.windSpeed)
            putNullable("windDirection", item.windDirection); putNullable("uvIndex", item.uvIndex)
            putNullable("sunrise", item.sunrise); putNullable("sunset", item.sunset)
        } })
        value.airQuality?.let { air -> put("airQuality", JSONObject().apply {
            put("standard", air.standard); putNullable("aqi", air.aqi); putNullable("pm25", air.pm25)
            putNullable("pm10", air.pm10); putNullable("nitrogenDioxide", air.nitrogenDioxide)
            putNullable("sulphurDioxide", air.sulphurDioxide); putNullable("ozone", air.ozone)
        }) }
        put("alerts", array(value.alerts) { item -> JSONObject().apply {
            put("title", item.title); put("severity", item.severity); put("description", item.description)
            put("source", item.source); putNullable("expiresAt", item.expiresAt)
        } })
        putNullable("alertSource", value.alertSource); put("alertsChecked", value.alertsChecked)
        put("alertStatusMessage", value.alertStatusMessage)
        put("solarDay", array(value.solarDay) { item -> JSONObject().put("time", item.time).put("elevation", item.elevation) })
    }

    private fun decode(root: JSONObject): WeatherDashboard {
        val current = root.getJSONObject("current")
        return WeatherDashboard(
            current = WeatherSnapshot(
                location = current.getString("location"), temperature = current.getDouble("temperature"),
                condition = current.getString("condition"), humidity = current.nullableDouble("humidity"),
                windSpeed = current.nullableDouble("windSpeed"), apparentTemperature = current.nullableDouble("apparentTemperature"),
                windDirection = current.nullableDouble("windDirection"), pressure = current.nullableDouble("pressure"),
                visibility = current.nullableDouble("visibility"), observationTime = current.nullableString("observationTime"),
                solarElevation = current.nullableDouble("solarElevation"), timezone = current.nullableString("timezone"),
                fetchedAtMillis = current.optLong("fetchedAtMillis", 0L),
            ),
            hourly = root.optJSONArray("hourly").objects().map { item -> HourlyForecast(
                item.getString("time"), item.getDouble("temperature"), item.getString("condition"),
                item.nullableDouble("precipitationProbability"), item.nullableDouble("precipitationAmount"),
                item.nullableDouble("windSpeed"), item.nullableDouble("windDirection"),
            ) },
            daily = root.optJSONArray("daily").objects().map { item -> DailyForecast(
                item.getString("date"), item.getDouble("high"), item.getDouble("low"), item.getString("condition"),
                item.nullableDouble("precipitationProbability"), item.nullableDouble("windSpeed"),
                item.nullableDouble("windDirection"), item.nullableDouble("uvIndex"),
                item.nullableString("sunrise"), item.nullableString("sunset"),
            ) },
            airQuality = root.optJSONObject("airQuality")?.let { item -> AirQuality(
                item.getString("standard"), item.nullableDouble("aqi"), item.nullableDouble("pm25"),
                item.nullableDouble("pm10"), item.nullableDouble("nitrogenDioxide"),
                item.nullableDouble("sulphurDioxide"), item.nullableDouble("ozone"),
            ) },
            alerts = root.optJSONArray("alerts").objects().map { item -> WeatherAlert(
                item.getString("title"), item.getString("severity"), item.getString("description"),
                item.getString("source"), item.nullableString("expiresAt"),
            ) },
            alertSource = root.nullableString("alertSource"),
            alertsChecked = root.optBoolean("alertsChecked"),
            alertStatusMessage = root.optString("alertStatusMessage", "该地区尚未配置官方预警数据源"),
            solarDay = root.optJSONArray("solarDay").objects().map { SolarPosition(it.getString("time"), it.getDouble("elevation")) },
        )
    }

    private fun <T> array(values: List<T>, encode: (T) -> JSONObject) = JSONArray().apply { values.forEach { put(encode(it)) } }
    private fun JSONObject.putNullable(key: String, value: Any?) { if (value != null) put(key, value) }
    private fun JSONObject.nullableDouble(key: String): Double? = if (has(key) && !isNull(key)) getDouble(key) else null
    private fun JSONObject.nullableString(key: String): String? = if (has(key) && !isNull(key)) getString(key) else null
    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull(::optJSONObject)
}
