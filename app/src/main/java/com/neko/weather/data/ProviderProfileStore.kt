package com.neko.weather.data

import android.content.Context
import com.neko.weather.model.AirProviderProfile
import com.neko.weather.model.AlertProviderProfile
import com.neko.weather.model.ProviderProfiles
import com.neko.weather.model.WeatherApiConfig
import com.neko.weather.model.WeatherProviderProfile
import com.neko.weather.model.defaultProviderProfiles
import org.json.JSONArray
import org.json.JSONObject

class ProviderProfileStore(context: Context) {
    private val preferences = context.getSharedPreferences("neko_weather", Context.MODE_PRIVATE)

    fun load(current: WeatherApiConfig): ProviderProfiles {
        val raw = preferences.getString(KEY, null) ?: return migrate(current).also(::save)
        return runCatching { parse(JSONObject(raw)) }.getOrElse { migrate(current).also(::save) }
    }

    fun save(value: ProviderProfiles) {
        val root = JSONObject()
            .put("selectedWeatherId", value.selectedWeatherId)
            .put("selectedAirId", value.selectedAirId)
            .put("selectedAlertId", value.selectedAlertId)
            .put("weather", JSONArray(value.weather.map(::weatherJson)))
            .put("air", JSONArray(value.air.map(::airJson)))
            .put("alerts", JSONArray(value.alerts.map(::alertJson)))
        preferences.edit().putString(KEY, root.toString()).apply()
    }

    private fun migrate(config: WeatherApiConfig): ProviderProfiles {
        val defaults = defaultProviderProfiles()
        val customWeather = !config.endpoint.contains("api.open-meteo.com/v1/forecast")
        val weatherProfile = WeatherProviderProfile(
            id = "migrated-weather",
            name = config.displayName.ifBlank { "已有天气配置" },
            endpoint = config.endpoint,
            latitudeParameter = config.latitudeParameter,
            longitudeParameter = config.longitudeParameter,
            apiKey = config.apiKey,
            apiKeyName = config.apiKeyName,
            apiKeyInHeader = config.apiKeyInHeader,
            extraQuery = config.extraQuery,
            temperaturePath = config.temperaturePath,
            conditionPath = config.conditionPath,
            humidityPath = config.humidityPath,
            windSpeedPath = config.windSpeedPath,
        )
        val customAir = defaults.air.none { it.endpoint == config.airQualityEndpoint }
        val customAlert = config.alertProvider == "CUSTOM"
        val alerts = defaults.alerts + if (customAlert) listOf(AlertProviderProfile("migrated-alert", "已有预警配置", mode = "CUSTOM", endpoint = config.alertEndpoint)) else emptyList()
        return defaults.copy(
            weather = defaults.weather + if (customWeather) listOf(weatherProfile) else emptyList(),
            air = defaults.air + if (customAir) listOf(AirProviderProfile("migrated-air", "已有空气质量配置", endpoint = config.airQualityEndpoint, standard = config.airQualityStandard)) else emptyList(),
            alerts = alerts,
            selectedWeatherId = if (customWeather) "migrated-weather" else defaults.selectedWeatherId,
            selectedAirId = if (customAir) "migrated-air" else defaults.selectedAirId,
            selectedAlertId = when {
                customAlert -> "migrated-alert"
                config.alertProvider == "QWEATHER" -> "builtin-qweather-alerts"
                config.alertProvider == "OFF" -> "builtin-off-alerts"
                else -> defaults.selectedAlertId
            },
        )
    }

    private fun parse(root: JSONObject): ProviderProfiles {
        val defaults = defaultProviderProfiles()
        val savedWeather = root.optJSONArray("weather").objects().map { item -> WeatherProviderProfile(
            id = item.getString("id"), name = item.getString("name"), builtIn = item.optBoolean("builtIn"),
            endpoint = item.getString("endpoint"), latitudeParameter = item.optString("latitudeParameter", "latitude"),
            longitudeParameter = item.optString("longitudeParameter", "longitude"), apiKey = item.optString("apiKey"),
            apiKeyName = item.optString("apiKeyName"), apiKeyInHeader = item.optBoolean("apiKeyInHeader"),
            extraQuery = item.optString("extraQuery"), temperaturePath = item.optString("temperaturePath", "current.temperature_2m"),
            conditionPath = item.optString("conditionPath", "current.weather_code"), humidityPath = item.optString("humidityPath", "current.relative_humidity_2m"),
            windSpeedPath = item.optString("windSpeedPath", "current.wind_speed_10m"),
        ) }
        val weather = (defaults.weather + savedWeather.filterNot { saved -> defaults.weather.any { it.id == saved.id } }).distinctBy { it.id }
        val savedAir = root.optJSONArray("air").objects().map { item -> AirProviderProfile(
            item.getString("id"), item.getString("name"), item.optBoolean("builtIn"), item.getString("endpoint"), item.optString("standard", "US"),
        ) }
        val air = (defaults.air + savedAir.filterNot { saved -> defaults.air.any { it.id == saved.id } }).distinctBy { it.id }
        val savedAlerts = root.optJSONArray("alerts").objects().map { item -> AlertProviderProfile(
            item.getString("id"), item.getString("name"), item.optBoolean("builtIn"), item.getString("mode"), item.optString("endpoint"),
        ) }
        val alerts = (defaults.alerts + savedAlerts.filterNot { saved -> defaults.alerts.any { it.id == saved.id } }).distinctBy { it.id }
        return ProviderProfiles(
            weather, air, alerts,
            root.optString("selectedWeatherId").takeIf { id -> weather.any { it.id == id } } ?: weather.first().id,
            root.optString("selectedAirId").takeIf { id -> air.any { it.id == id } } ?: air.first().id,
            root.optString("selectedAlertId").takeIf { id -> alerts.any { it.id == id } } ?: alerts.first().id,
        )
    }

    private fun weatherJson(it: WeatherProviderProfile) = JSONObject()
        .put("id", it.id).put("name", it.name).put("builtIn", it.builtIn).put("endpoint", it.endpoint)
        .put("latitudeParameter", it.latitudeParameter).put("longitudeParameter", it.longitudeParameter)
        .put("apiKey", it.apiKey).put("apiKeyName", it.apiKeyName).put("apiKeyInHeader", it.apiKeyInHeader)
        .put("extraQuery", it.extraQuery).put("temperaturePath", it.temperaturePath).put("conditionPath", it.conditionPath)
        .put("humidityPath", it.humidityPath).put("windSpeedPath", it.windSpeedPath)

    private fun airJson(it: AirProviderProfile) = JSONObject().put("id", it.id).put("name", it.name)
        .put("builtIn", it.builtIn).put("endpoint", it.endpoint).put("standard", it.standard)

    private fun alertJson(it: AlertProviderProfile) = JSONObject().put("id", it.id).put("name", it.name)
        .put("builtIn", it.builtIn).put("mode", it.mode).put("endpoint", it.endpoint)

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else
        (0 until length()).mapNotNull(::optJSONObject)

    private companion object { const val KEY = "providerProfiles" }
}
