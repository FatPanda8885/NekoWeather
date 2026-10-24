package com.neko.weather.model

import androidx.compose.ui.graphics.Color

data class WeatherApiConfig(
    val displayName: String = "Open-Meteo",
    val endpoint: String = "https://api.open-meteo.com/v1/forecast",
    val latitude: String = "31.2304",
    val longitude: String = "121.4737",
    val locationName: String = "上海",
    val locationAdmin: String = "上海市",
    val locationCountry: String = "中国",
    val latitudeParameter: String = "latitude",
    val longitudeParameter: String = "longitude",
    val apiKey: String = "",
    val apiKeyName: String = "",
    val apiKeyInHeader: Boolean = false,
    val airQualityStandard: String = "US",
    val airQualityEndpoint: String = "https://air-quality-api.open-meteo.com/v1/air-quality",
    val alertProvider: String = "AUTO",
    val alertEndpoint: String = "",
    val extraQuery: String = DEFAULT_OPEN_METEO_QUERY,
    val temperaturePath: String = "current.temperature_2m",
    val conditionPath: String = "current.weather_code",
    val humidityPath: String = "current.relative_humidity_2m",
    val windSpeedPath: String = "current.wind_speed_10m",
    val qWeatherApiHost: String = "devapi.qweather.com",
    val qWeatherApiKey: String = "",
    val radarMapProvider: String = "AUTO",
    val amapWebApiKey: String = "",
    val petalMapApiKey: String = "",
    val baiduMapAk: String = "",
)

const val DEFAULT_OPEN_METEO_QUERY =
    "current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code," +
        "surface_pressure,wind_speed_10m,wind_direction_10m,visibility&" +
        "hourly=temperature_2m,weather_code,precipitation_probability,precipitation,wind_speed_10m," +
        "wind_direction_10m,relative_humidity_2m,apparent_temperature,visibility,surface_pressure&" +
        "daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max," +
        "wind_speed_10m_max,wind_direction_10m_dominant,uv_index_max,sunrise,sunset&" +
        "past_days=1&forecast_days=15&timezone=auto"

data class WeatherSnapshot(
    val location: String,
    val temperature: Double,
    val condition: String,
    val humidity: Double?,
    val windSpeed: Double?,
    val apparentTemperature: Double? = null,
    val windDirection: Double? = null,
    val pressure: Double? = null,
    val visibility: Double? = null,
    val observationTime: String? = null,
    val solarElevation: Double? = null,
    val timezone: String? = null,
    val fetchedAtMillis: Long = System.currentTimeMillis(),
) {
    fun speechText(): String = buildString {
        append(location)
        append("当前")
        append(condition)
        append("，气温")
        append(formatNumber(temperature))
        append("摄氏度")
        humidity?.let {
            append("，相对湿度百分之")
            append(formatNumber(it))
        }
        windSpeed?.let {
            append("，风速每小时")
            append(formatNumber(it))
            append("公里")
        }
        append("。")
    }

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
}

data class HourlyForecast(
    val time: String,
    val temperature: Double,
    val condition: String,
    val precipitationProbability: Double?,
    val precipitationAmount: Double?,
    val windSpeed: Double?,
    val windDirection: Double?,
)

data class SolarPosition(
    val time: String,
    val elevation: Double,
)

data class DailyForecast(
    val date: String,
    val high: Double,
    val low: Double,
    val condition: String,
    val precipitationProbability: Double?,
    val windSpeed: Double?,
    val windDirection: Double?,
    val uvIndex: Double?,
    val sunrise: String?,
    val sunset: String?,
)

data class AirQuality(
    val standard: String,
    val aqi: Double?,
    val pm25: Double?,
    val pm10: Double?,
    val nitrogenDioxide: Double?,
    val sulphurDioxide: Double?,
    val ozone: Double?,
)

data class WeatherAlert(
    val title: String,
    val severity: String,
    val description: String,
    val source: String,
    val expiresAt: String? = null,
)

data class WeatherDashboard(
    val current: WeatherSnapshot,
    val hourly: List<HourlyForecast> = emptyList(),
    val daily: List<DailyForecast> = emptyList(),
    val airQuality: AirQuality? = null,
    val alerts: List<WeatherAlert> = emptyList(),
    val alertSource: String? = null,
    val alertsChecked: Boolean = false,
    val alertStatusMessage: String = "该地区尚未配置官方预警数据源",
    val solarDay: List<SolarPosition> = emptyList(),
)

data class VoiceSettings(
    val rate: Float = 1f,
    val pitch: Float = 1f,
)

data class NotificationSettings(
    val morningEnabled: Boolean = true,
    val weeklyEnabled: Boolean = true,
    val rainEnabled: Boolean = true,
    val alertEnabled: Boolean = true,
)

data class WeatherRefreshSettings(
    val enabled: Boolean = true,
    val intervalMinutes: Int = 30,
    val freshnessGuardEnabled: Boolean = true,
    val freshnessMinutes: Int = 5,
)

enum class TemperatureUnit { CELSIUS, FAHRENHEIT, KELVIN }
enum class WindSpeedUnit { KILOMETRES_PER_HOUR, METRES_PER_SECOND }
enum class AppLanguage { SIMPLIFIED_CHINESE, TRADITIONAL_CHINESE, ENGLISH, JAPANESE }

/** 主题模式：AUTO 跟随系统深色模式。 */
enum class ThemeMode { LIGHT, DARK, AUTO }

data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.AUTO,
    val useDynamicColor: Boolean = false,
    /** ARGB 颜色值（0xAARRGGBB），经 toArgb 往返以保证与色板颜色可比。 */
    val seedColorValue: Long = 0xFF3568D4,
) {
    val seedColor: Color get() = Color(seedColorValue.toInt())

    companion object {
        fun modeFromName(raw: String): ThemeMode =
            ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.AUTO
    }
}

/** 主题色色板：以 HCT 色彩空间按色相均分派生（hue = base * 35）。 */
fun themeColorOptions(): List<Color> = (1..10).map { base ->
    Color(com.materialkolor.hct.Hct.from(base * 35.0, 40.0, 40.0).toInt())
} + Color(0xFF3568D4)

data class DisplaySettings(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val windSpeedUnit: WindSpeedUnit = WindSpeedUnit.KILOMETRES_PER_HOUR,
    val language: AppLanguage = AppLanguage.SIMPLIFIED_CHINESE,
    /** 空串表示跟随系统时区，否则为 IANA 时区 ID（如 Asia/Shanghai）。 */
    val timeZoneId: String = "",
)

/** 外观预设：皮肤四类资源的组合，可一键切换、导入导出。 */
data class AppearancePreset(
    val id: String,
    val name: String,
    val fontPath: String = "",
    val logoPath: String = "",
    val backgroundPath: String = "",
    val backgroundIsVideo: Boolean = false,
    val weatherBackgrounds: Map<String, String> = emptyMap(),
) {
    /** 资源摘要，例如「字体 · Logo · 天气背景 ×2」。 */
    fun summary(): String = buildList {
        if (fontPath.isNotBlank()) add("字体")
        if (logoPath.isNotBlank()) add("Logo")
        if (backgroundPath.isNotBlank()) add(if (backgroundIsVideo) "视频背景" else "静态背景")
        if (weatherBackgrounds.isNotEmpty()) add("天气背景 ×${weatherBackgrounds.size}")
    }.joinToString(" · ").ifBlank { "默认外观" }
}

data class AppearancePresets(
    val presets: List<AppearancePreset> = emptyList(),
    val selectedId: String = "",
) {
    val selected: AppearancePreset? get() = presets.firstOrNull { it.id == selectedId } ?: presets.firstOrNull()
}

const val DEFAULT_AI_CONTEXT = "你是专业天气生活顾问。请用简洁中文给出有依据、可执行的建议，不要夸大风险。"

const val DEFAULT_ANTHROPIC_REQUEST_JSON =
    """{"model":{{model_json}},"max_tokens":{{max_tokens}},"system":{{context_json}},"messages":[{"role":"user","content":{{weather_json}}}]}"""

const val DEFAULT_OPENAI_REQUEST_JSON =
    """{"model":{{model_json}},"max_tokens":{{max_tokens}},"messages":[{"role":"system","content":{{context_json}}},{"role":"user","content":{{weather_json}}}]}"""

const val DEFAULT_AI_REQUEST_JSON = DEFAULT_ANTHROPIC_REQUEST_JSON

/** 请求协议：决定默认地址、默认请求体模板、请求头与响应解析方式。 */
enum class AiProtocol(val label: String, val defaultEndpoint: String, val defaultRequestJson: String) {
    ANTHROPIC_MESSAGES("Anthropic", "https://api.anthropic.com/v1/messages", DEFAULT_ANTHROPIC_REQUEST_JSON),
    OPENAI_CHAT("OpenAI 兼容", "https://api.openai.com/v1/chat/completions", DEFAULT_OPENAI_REQUEST_JSON);

    companion object {
        /** 未知（例如旧版本写入）协议名回退到 Anthropic。 */
        fun fromName(raw: String): AiProtocol = entries.firstOrNull { it.name == raw } ?: ANTHROPIC_MESSAGES
    }
}

data class AiModelProfile(
    val id: String,
    val name: String,
    val protocol: AiProtocol = AiProtocol.ANTHROPIC_MESSAGES,
    val endpoint: String = protocol.defaultEndpoint,
    val apiKey: String = "",
    val model: String = "",
    val maxTokens: Int = 800,
    val maxOutputCharacters: Int = 1200,
    val context: String = DEFAULT_AI_CONTEXT,
    val rawRequestJson: String = protocol.defaultRequestJson,
)

data class AiProfiles(
    val models: List<AiModelProfile> = emptyList(),
    val selectedId: String = "",
) {
    val selected: AiModelProfile? get() = models.firstOrNull { it.id == selectedId } ?: models.firstOrNull()
}

data class AiSettings(
    val endpoint: String = "https://api.anthropic.com/v1/messages",
    val apiKey: String = "",
    val model: String = "claude-3-5-haiku-latest",
    val maxTokens: Int = 800,
    val maxOutputCharacters: Int = 1200,
    val context: String = DEFAULT_AI_CONTEXT,
    val rawRequestJson: String = DEFAULT_ANTHROPIC_REQUEST_JSON,
)

data class WeatherProviderProfile(
    val id: String,
    val name: String,
    val builtIn: Boolean = false,
    val endpoint: String,
    val latitudeParameter: String = "latitude",
    val longitudeParameter: String = "longitude",
    val apiKey: String = "",
    val apiKeyName: String = "",
    val apiKeyInHeader: Boolean = false,
    val extraQuery: String = "",
    val temperaturePath: String = "current.temperature_2m",
    val conditionPath: String = "current.weather_code",
    val humidityPath: String = "current.relative_humidity_2m",
    val windSpeedPath: String = "current.wind_speed_10m",
)

data class AirProviderProfile(
    val id: String,
    val name: String,
    val builtIn: Boolean = false,
    val endpoint: String,
    val standard: String = "US",
)

data class AlertProviderProfile(
    val id: String,
    val name: String,
    val builtIn: Boolean = false,
    val mode: String,
    val endpoint: String = "",
)

data class ProviderProfiles(
    val weather: List<WeatherProviderProfile>,
    val air: List<AirProviderProfile>,
    val alerts: List<AlertProviderProfile>,
    val selectedWeatherId: String,
    val selectedAirId: String,
    val selectedAlertId: String,
)

fun defaultProviderProfiles() = ProviderProfiles(
    weather = listOf(
        WeatherProviderProfile(
            id = "builtin-qweather",
            name = "和风天气",
            builtIn = true,
            endpoint = "https://devapi.qweather.com/v7/weather/now",
            latitudeParameter = "",
            longitudeParameter = "",
            temperaturePath = "now.temp",
            conditionPath = "now.text",
            humidityPath = "now.humidity",
            windSpeedPath = "now.windSpeed",
        ),
        WeatherProviderProfile(
            id = "builtin-open-meteo",
            name = "Open-Meteo",
            builtIn = true,
            endpoint = "https://api.open-meteo.com/v1/forecast",
            extraQuery = DEFAULT_OPEN_METEO_QUERY,
        ),
    ),
    air = listOf(
        AirProviderProfile(
            id = "builtin-qweather-air",
            name = "和风天气空气质量",
            builtIn = true,
            endpoint = "https://devapi.qweather.com/airquality/v1/current",
        ),
        AirProviderProfile(
            id = "builtin-open-meteo-air",
            name = "Open-Meteo / CAMS",
            builtIn = true,
            endpoint = "https://air-quality-api.open-meteo.com/v1/air-quality",
        ),
    ),
    alerts = listOf(
        AlertProviderProfile(
            id = "builtin-qweather-alerts",
            name = "和风天气气象预警",
            builtIn = true,
            mode = "QWEATHER",
            endpoint = "https://devapi.qweather.com/weatheralert/v1/current",
        ),
        AlertProviderProfile(
            id = "builtin-auto-alerts",
            name = "自动选择官方源",
            builtIn = true,
            mode = "AUTO",
        ),
        AlertProviderProfile(
            id = "builtin-off-alerts",
            name = "关闭气象预警",
            builtIn = true,
            mode = "OFF",
        ),
    ),
    selectedWeatherId = "builtin-open-meteo",
    selectedAirId = "builtin-open-meteo-air",
    selectedAlertId = "builtin-auto-alerts",
)

data class UpdateInfo(
    val version: String,
    val changelog: String,
    val pageUrl: String,
    val downloadUrl: String?,
    val source: String,
    val updateAvailable: Boolean,
)

data class Live2dPlacement(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val scale: Float = 1f,
    val rotation: Float = 0f,
)

data class Live2dModelProfile(
    val id: String,
    val name: String,
    val modelPath: String,
    val previewPath: String = "",
    val placement: Live2dPlacement = Live2dPlacement(),
)

data class CustomizationSettings(
    val fontPath: String = "",
    val logoPath: String = "",
    val backgroundPath: String = "",
    val backgroundIsVideo: Boolean = false,
    val weatherBackgrounds: Map<String, String> = emptyMap(),
    val appIconPath: String = "",
    val live2dModelPath: String = "",
    val live2dPreviewPath: String = "",
    val live2dModels: List<Live2dModelProfile> = emptyList(),
    val selectedLive2dId: String = "",
    val live2dEnabled: Boolean = true,
    val live2dHidden: Boolean = false,
)

data class ImportedVoiceModel(
    val id: String,
    val name: String,
    val engine: String,
    val language: String,
    val sampleRate: Int,
    val directoryPath: String,
)

data class CityLocation(
    val id: String,
    val name: String,
    val admin: String = "",
    val country: String = "",
    val latitude: Double,
    val longitude: Double,
    val isCurrentLocation: Boolean = false,
    val showOnHome: Boolean = true,
) {
    val subtitle: String
        get() = listOf(admin, country).filter(String::isNotBlank).distinct().joinToString(" · ")
}

val HOT_CITIES = listOf(
    CityLocation("cn-beijing", "北京", "北京市", "中国", 39.9042, 116.4074),
    CityLocation("cn-shanghai", "上海", "上海市", "中国", 31.2304, 121.4737),
    CityLocation("cn-guangzhou", "广州", "广东省", "中国", 23.1291, 113.2644),
    CityLocation("cn-shenzhen", "深圳", "广东省", "中国", 22.5431, 114.0579),
    CityLocation("cn-chengdu", "成都", "四川省", "中国", 30.5728, 104.0668),
    CityLocation("cn-hangzhou", "杭州", "浙江省", "中国", 30.2741, 120.1551),
    CityLocation("cn-wuhan", "武汉", "湖北省", "中国", 30.5928, 114.3055),
    CityLocation("cn-xian", "西安", "陕西省", "中国", 34.3416, 108.9398),
    CityLocation("cn-chongqing", "重庆", "重庆市", "中国", 29.5630, 106.5516),
    CityLocation("cn-nanjing", "南京", "江苏省", "中国", 32.0603, 118.7969),
    CityLocation("cn-suzhou", "苏州", "江苏省", "中国", 31.2989, 120.5853),
    CityLocation("cn-qingdao", "青岛", "山东省", "中国", 36.0671, 120.3826),
)
