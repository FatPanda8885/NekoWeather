package com.neko.weather.data

import android.content.Context
import com.neko.weather.model.VoiceSettings
import com.neko.weather.model.WeatherApiConfig
import com.neko.weather.model.DEFAULT_OPEN_METEO_QUERY
import com.neko.weather.model.NotificationSettings
import com.neko.weather.model.DisplaySettings
import com.neko.weather.model.TemperatureUnit
import com.neko.weather.model.WeatherRefreshSettings
import com.neko.weather.model.AiSettings
import com.neko.weather.model.DEFAULT_AI_REQUEST_JSON

class ConfigStore(context: Context) {
    init { ApiUsageTracker.initialize(context) }
    private val preferences = context.getSharedPreferences("neko_weather", Context.MODE_PRIVATE)

    fun isOnboardingCompleted(): Boolean = preferences.getBoolean("onboardingCompleted", false)

    fun markOnboardingCompleted() {
        preferences.edit().putBoolean("onboardingCompleted", true).apply()
    }

    fun loadWeatherConfig(): WeatherApiConfig = WeatherApiConfig(
        displayName = get("displayName", "Open-Meteo"),
        endpoint = get("endpoint", "https://api.open-meteo.com/v1/forecast"),
        latitude = get("latitude", "31.2304"),
        longitude = get("longitude", "121.4737"),
        locationName = get("locationName", "上海"),
        locationAdmin = get("locationAdmin", ""),
        locationCountry = get("locationCountry", ""),
        latitudeParameter = get("latitudeParameter", "latitude"),
        longitudeParameter = get("longitudeParameter", "longitude"),
        apiKey = get("apiKey", ""),
        apiKeyName = get("apiKeyName", ""),
        apiKeyInHeader = preferences.getBoolean("apiKeyInHeader", false),
        airQualityStandard = get("airQualityStandard", "US"),
        airQualityEndpoint = get("airQualityEndpoint", "https://air-quality-api.open-meteo.com/v1/air-quality"),
        alertProvider = get("alertProvider", "AUTO"),
        alertEndpoint = get("alertEndpoint", ""),
        extraQuery = get("extraQuery", DEFAULT_OPEN_METEO_QUERY).let {
            if (
                it == "current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m&timezone=auto" ||
                (it.contains("forecast_days=16") && !it.contains("past_days="))
            ) {
                DEFAULT_OPEN_METEO_QUERY
            } else it.replace(
                "precipitation_probability,wind_speed_10m",
                "precipitation_probability,precipitation,wind_speed_10m",
            )
        },
        temperaturePath = get("temperaturePath", "current.temperature_2m"),
        conditionPath = get("conditionPath", "current.weather_code"),
        humidityPath = get("humidityPath", "current.relative_humidity_2m"),
        windSpeedPath = get("windSpeedPath", "current.wind_speed_10m"),
        qWeatherApiHost = get("qWeatherApiHost", "devapi.qweather.com"),
        qWeatherApiKey = get("qWeatherApiKey", ""),
        radarMapProvider = get("radarMapProvider", "AUTO"),
        amapWebApiKey = get("amapWebApiKey", ""),
        petalMapApiKey = get("petalMapApiKey", ""),
        baiduMapAk = get("baiduMapAk", ""),
    )

    fun saveWeatherConfig(config: WeatherApiConfig) {
        preferences.edit()
            .putString("displayName", config.displayName)
            .putString("endpoint", config.endpoint)
            .putString("latitude", config.latitude)
            .putString("longitude", config.longitude)
            .putString("locationName", config.locationName)
            .putString("locationAdmin", config.locationAdmin)
            .putString("locationCountry", config.locationCountry)
            .putString("latitudeParameter", config.latitudeParameter)
            .putString("longitudeParameter", config.longitudeParameter)
            .putString("apiKey", config.apiKey)
            .putString("apiKeyName", config.apiKeyName)
            .putBoolean("apiKeyInHeader", config.apiKeyInHeader)
            .putString("airQualityStandard", config.airQualityStandard)
            .putString("airQualityEndpoint", config.airQualityEndpoint)
            .putString("alertProvider", config.alertProvider)
            .putString("alertEndpoint", config.alertEndpoint)
            .putString("extraQuery", config.extraQuery)
            .putString("temperaturePath", config.temperaturePath)
            .putString("conditionPath", config.conditionPath)
            .putString("humidityPath", config.humidityPath)
            .putString("windSpeedPath", config.windSpeedPath)
            .putString("qWeatherApiHost", config.qWeatherApiHost)
            .putString("qWeatherApiKey", config.qWeatherApiKey)
            .putString("radarMapProvider", config.radarMapProvider)
            .putString("amapWebApiKey", config.amapWebApiKey)
            .putString("petalMapApiKey", config.petalMapApiKey)
            .putString("baiduMapAk", config.baiduMapAk)
            .apply()
    }

    fun loadVoiceSettings() = VoiceSettings(
        rate = preferences.getFloat("voiceRate", 1f),
        pitch = preferences.getFloat("voicePitch", 1f),
    )

    fun saveVoiceSettings(settings: VoiceSettings) {
        preferences.edit()
            .putFloat("voiceRate", settings.rate)
            .putFloat("voicePitch", settings.pitch)
            .apply()
    }

    fun loadNotificationSettings() = NotificationSettings(
        morningEnabled = preferences.getBoolean("notifyMorning", true),
        weeklyEnabled = preferences.getBoolean("notifyWeekly", true),
        rainEnabled = preferences.getBoolean("notifyRain", true),
        alertEnabled = preferences.getBoolean("notifyAlert", true),
    )

    fun saveNotificationSettings(settings: NotificationSettings) {
        preferences.edit()
            .putBoolean("notifyMorning", settings.morningEnabled)
            .putBoolean("notifyWeekly", settings.weeklyEnabled)
            .putBoolean("notifyRain", settings.rainEnabled)
            .putBoolean("notifyAlert", settings.alertEnabled)
            .apply()
    }

    fun loadWeatherRefreshSettings() = WeatherRefreshSettings(
        enabled = preferences.getBoolean("autoRefreshEnabled", true),
        intervalMinutes = preferences.getInt("autoRefreshMinutes", 30).coerceIn(5, 120),
        freshnessGuardEnabled = preferences.getBoolean("freshnessGuardEnabled", true),
        freshnessMinutes = preferences.getInt("freshnessMinutes", 5).coerceIn(1, 120),
    )

    fun saveWeatherRefreshSettings(settings: WeatherRefreshSettings) {
        preferences.edit()
            .putBoolean("autoRefreshEnabled", settings.enabled)
            .putInt("autoRefreshMinutes", settings.intervalMinutes.coerceIn(5, 120))
            .putBoolean("freshnessGuardEnabled", settings.freshnessGuardEnabled)
            .putInt("freshnessMinutes", settings.freshnessMinutes.coerceIn(1, 120))
            .apply()
    }

    fun loadLastWeatherUpdateMillis(): Long = preferences.getLong("lastWeatherUpdateMillis", 0L)

    fun saveLastWeatherUpdateMillis(value: Long = System.currentTimeMillis()) {
        preferences.edit().putLong("lastWeatherUpdateMillis", value).apply()
    }

    fun loadDisplaySettings() = DisplaySettings(
        temperatureUnit = runCatching {
            TemperatureUnit.valueOf(get("temperatureUnit", TemperatureUnit.CELSIUS.name))
        }.getOrDefault(TemperatureUnit.CELSIUS),
        windSpeedUnit = runCatching {
            com.neko.weather.model.WindSpeedUnit.valueOf(get("windSpeedUnit", com.neko.weather.model.WindSpeedUnit.KILOMETRES_PER_HOUR.name))
        }.getOrDefault(com.neko.weather.model.WindSpeedUnit.KILOMETRES_PER_HOUR),
        language = runCatching {
            com.neko.weather.model.AppLanguage.valueOf(get("appLanguage", com.neko.weather.model.AppLanguage.SIMPLIFIED_CHINESE.name))
        }.getOrDefault(com.neko.weather.model.AppLanguage.SIMPLIFIED_CHINESE),
    )

    fun saveDisplaySettings(settings: DisplaySettings) {
        preferences.edit()
            .putString("temperatureUnit", settings.temperatureUnit.name)
            .putString("windSpeedUnit", settings.windSpeedUnit.name)
            .putString("appLanguage", settings.language.name)
            .apply()
    }

    fun loadAiSettings() = AiSettings(
        endpoint = get("aiEndpoint", "https://api.anthropic.com/v1/messages"),
        apiKey = get("aiApiKey", ""),
        model = get("aiModel", "claude-3-5-haiku-latest"),
        maxTokens = preferences.getInt("aiMaxTokens", 800).coerceIn(64, 8192),
        maxOutputCharacters = preferences.getInt("aiMaxOutputCharacters", 1200).coerceIn(100, 10000),
        context = get("aiContext", "你是专业天气生活顾问。请用简洁中文给出有依据、可执行的建议，不要夸大风险。"),
        rawRequestJson = get("aiRawRequestJson", DEFAULT_AI_REQUEST_JSON),
    )

    private fun get(key: String, default: String): String = preferences.getString(key, default) ?: default
}
