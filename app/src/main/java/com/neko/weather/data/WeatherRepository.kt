package com.neko.weather.data

import com.neko.weather.model.WeatherApiConfig
import com.neko.weather.model.AirQuality
import com.neko.weather.model.DailyForecast
import com.neko.weather.model.HourlyForecast
import com.neko.weather.model.WeatherDashboard
import com.neko.weather.model.WeatherSnapshot
import com.neko.weather.model.SolarPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.time.LocalDateTime
import java.time.OffsetDateTime
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

class WeatherRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(ApiUsageInterceptor())
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
) {
    private val alertRepository = AlertRepository(client)

    suspend fun fetch(config: WeatherApiConfig): WeatherDashboard = withContext(Dispatchers.IO) {
        if (config.endpoint.contains("qweather.com") || config.displayName == "和风天气") {
            val dashboard = fetchQWeatherDashboard(config)
            val alertResult = alertRepository.fetch(config)
            return@withContext withNowHourlySlot(dashboard).copy(
                alerts = alertResult.alerts,
                alertSource = alertResult.source,
                alertsChecked = alertResult.checked,
                alertStatusMessage = alertResult.message,
            )
        }
        val url = config.endpoint.toHttpUrlOrNull()?.newBuilder()
            ?: throw IllegalArgumentException("API 地址格式不正确")

        if (config.latitudeParameter.isNotBlank()) url.setQueryParameter(config.latitudeParameter, config.latitude)
        if (config.longitudeParameter.isNotBlank()) url.setQueryParameter(config.longitudeParameter, config.longitude)
        parseQuery(config.extraQuery).forEach { (name, value) -> url.setQueryParameter(name, value) }
        if (config.apiKey.isNotBlank() && config.apiKeyName.isNotBlank() && !config.apiKeyInHeader) {
            url.setQueryParameter(config.apiKeyName, config.apiKey)
        }

        val request = Request.Builder().url(url.build()).get().apply {
            header("Accept", "application/json")
            header("User-Agent", "NekoWeather/0.1")
            if (config.apiKey.isNotBlank() && config.apiKeyName.isNotBlank() && config.apiKeyInHeader) {
                header(config.apiKeyName, config.apiKey)
            }
        }.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("天气接口返回 HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw IOException("天气接口返回了空内容")
            val json = JSONObject(body)
            val dashboard = if (config.endpoint.contains("open-meteo.com")) {
                parseOpenMeteoDashboard(json, config).copy(airQuality = fetchConfiguredAirQuality(config))
            } else {
                WeatherDashboard(current = parseSnapshot(json, config))
            }
            val alertResult = alertRepository.fetch(config)
            withNowHourlySlot(dashboard).copy(
                alerts = alertResult.alerts,
                alertSource = alertResult.source,
                alertsChecked = alertResult.checked,
                alertStatusMessage = alertResult.message,
            )
        }
    }

    /**
     * 逐小时条带的第一格固定显示「现在」，其真实时间标签会被吃掉。无论数据源自哪个
     * 供应商，都用当前实况补一个「现在」槽，并去掉与当前小时重复的整点条目，
     * 保证序列始终是：现在 → 下一小时 → 再下一小时。
     */
    private fun withNowHourlySlot(dashboard: WeatherDashboard): WeatherDashboard {
        val hourly = dashboard.hourly
        if (hourly.isEmpty()) return dashboard
        val current = dashboard.current
        val nowHour = current.observationTime.orEmpty().take(13)
        val rest = hourly.filterNot { it.time.take(13) == nowHour && nowHour.isNotBlank() }
        val nowEntry = HourlyForecast(
            time = current.observationTime ?: hourly.first().time,
            temperature = current.temperature,
            condition = current.condition,
            precipitationProbability = null,
            precipitationAmount = null,
            windSpeed = null,
            windDirection = null,
        )
        return dashboard.copy(hourly = listOf(nowEntry) + rest)
    }

    private fun fetchQWeatherDashboard(config: WeatherApiConfig): WeatherDashboard {
        require(config.qWeatherApiKey.isNotBlank()) { "请先在内容供应商中填写和风天气 API Key" }
        val location = "${config.longitude},${config.latitude}"
        val nowRoot = requestQWeather(config, "/v7/weather/now", mapOf("location" to location, "lang" to "zh"))
        val hourlyRoot = requestQWeather(config, "/v7/weather/24h", mapOf("location" to location, "lang" to "zh"))
        val dailyRoot = requestQWeather(config, "/v7/weather/15d", mapOf("location" to location, "lang" to "zh"))
        val now = nowRoot.optJSONObject("now") ?: throw IOException("和风天气实时天气响应缺少 now")
        val observationRaw = now.optString("obsTime")
        val observationTime = observationRaw.take(16).takeIf { it.length == 16 }
        val utcOffsetSeconds = runCatching { OffsetDateTime.parse(observationRaw).offset.totalSeconds }.getOrDefault(0)
        val current = WeatherSnapshot(
            location = config.locationName,
            temperature = now.optString("temp").toDoubleOrNull() ?: throw IOException("和风天气响应缺少温度"),
            condition = now.optString("text", "天气未知"),
            humidity = now.optString("humidity").toDoubleOrNull(),
            windSpeed = now.optString("windSpeed").toDoubleOrNull(),
            apparentTemperature = now.optString("feelsLike").toDoubleOrNull(),
            windDirection = now.optString("wind360").toDoubleOrNull(),
            pressure = now.optString("pressure").toDoubleOrNull(),
            visibility = now.optString("vis").toDoubleOrNull(),
            observationTime = observationTime,
            solarElevation = calculateSolarElevation(observationTime.orEmpty(), config.latitude.toDoubleOrNull(), config.longitude.toDoubleOrNull(), utcOffsetSeconds),
            // 记录所在地时区（固定偏移形式），供显示时区换算使用
            timezone = java.time.ZoneOffset.ofTotalSeconds(utcOffsetSeconds).id.takeIf { observationTime != null },
        )
        val hourlyArray = hourlyRoot.optJSONArray("hourly")
        val hourly = (0 until minOf(24, hourlyArray?.length() ?: 0)).mapNotNull { index ->
            val item = hourlyArray?.optJSONObject(index) ?: return@mapNotNull null
            val temperature = item.optString("temp").toDoubleOrNull() ?: return@mapNotNull null
            HourlyForecast(
                time = item.optString("fxTime").take(16),
                temperature = temperature,
                condition = item.optString("text", "天气未知"),
                precipitationProbability = item.optString("pop").toDoubleOrNull(),
                precipitationAmount = item.optString("precip").toDoubleOrNull(),
                windSpeed = item.optString("windSpeed").toDoubleOrNull(),
                windDirection = item.optString("wind360").toDoubleOrNull(),
            )
        }
        val dailyArray = dailyRoot.optJSONArray("daily")
        val daily = (0 until minOf(15, dailyArray?.length() ?: 0)).mapNotNull { index ->
            val item = dailyArray?.optJSONObject(index) ?: return@mapNotNull null
            val date = item.optString("fxDate")
            val high = item.optString("tempMax").toDoubleOrNull() ?: return@mapNotNull null
            val low = item.optString("tempMin").toDoubleOrNull() ?: return@mapNotNull null
            DailyForecast(
                date = date,
                high = high,
                low = low,
                condition = item.optString("textDay", "天气未知"),
                precipitationProbability = null,
                windSpeed = item.optString("windSpeedDay").toDoubleOrNull(),
                windDirection = item.optString("wind360Day").toDoubleOrNull(),
                uvIndex = item.optString("uvIndex").toDoubleOrNull(),
                sunrise = item.optString("sunrise").takeIf(String::isNotBlank)?.let { "${date}T$it" },
                sunset = item.optString("sunset").takeIf(String::isNotBlank)?.let { "${date}T$it" },
            )
        }
        val today = daily.firstOrNull()
        val sunrise = today?.sunrise?.takeLast(5)?.split(':')?.firstOrNull()?.toIntOrNull()
        val sunset = today?.sunset?.takeLast(5)?.split(':')?.firstOrNull()?.toIntOrNull()
        val solarDay = if (today != null && sunrise != null && sunset != null) {
            (sunrise..sunset).mapNotNull { hour ->
                val time = "${today.date}T${hour.toString().padStart(2, '0')}:00"
                calculateSolarElevation(time, config.latitude.toDoubleOrNull(), config.longitude.toDoubleOrNull(), utcOffsetSeconds)?.let { SolarPosition(time, it) }
            }
        } else emptyList()
        return WeatherDashboard(current, hourly, daily, fetchConfiguredAirQuality(config), solarDay = solarDay)
    }

    private fun fetchConfiguredAirQuality(config: WeatherApiConfig): AirQuality? =
        if (config.airQualityEndpoint.contains("/airquality/v1/")) fetchQWeatherAirQuality(config) else fetchOpenMeteoAirQuality(config)

    private fun fetchQWeatherAirQuality(config: WeatherApiConfig): AirQuality? = runCatching {
        val latitude = "%.2f".format(java.util.Locale.US, config.latitude.toDouble())
        val longitude = "%.2f".format(java.util.Locale.US, config.longitude.toDouble())
        val root = requestQWeather(config, "/airquality/v1/current/$latitude/$longitude", mapOf("lang" to "zh"))
        val indexes = root.optJSONArray("indexes")
        val preferredCode = if (config.airQualityStandard == "EU") "eu-caqi" else "us-epa"
        val index = (0 until (indexes?.length() ?: 0)).mapNotNull { indexes?.optJSONObject(it) }
            .firstOrNull { it.optString("code") == preferredCode }
            ?: indexes?.optJSONObject(0)
        val pollutants = root.optJSONArray("pollutants")
        fun pollutant(code: String): Double? = (0 until (pollutants?.length() ?: 0))
            .mapNotNull { pollutants?.optJSONObject(it) }.firstOrNull { it.optString("code") == code }
            ?.optJSONObject("concentration")?.optDouble("value", Double.NaN)?.takeIf(Double::isFinite)
        AirQuality(
            standard = index?.optString("code") ?: "QWeather",
            aqi = index?.optDouble("aqi", Double.NaN)?.takeIf(Double::isFinite),
            pm25 = pollutant("pm2p5"), pm10 = pollutant("pm10"), nitrogenDioxide = pollutant("no2"),
            sulphurDioxide = pollutant("so2"), ozone = pollutant("o3"),
        )
    }.getOrNull()

    private fun requestQWeather(config: WeatherApiConfig, path: String, query: Map<String, String>): JSONObject {
        val host = config.qWeatherApiHost.trim().removePrefix("https://").removePrefix("http://").trimEnd('/')
        val base = "https://$host".toHttpUrlOrNull() ?: throw IllegalArgumentException("和风天气 API Host 格式不正确")
        val builder = base.newBuilder().encodedPath(path)
        query.forEach(builder::setQueryParameter)
        val request = Request.Builder().url(builder.build()).header("X-QW-Api-Key", config.qWeatherApiKey)
            // 不手动设置 Accept-Encoding；OkHttp 会自动协商并透明解压 Gzip 响应。
            .header("Accept", "application/json").header("User-Agent", "NekoWeather/0.3").build()
        return client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw IOException("和风天气接口返回 HTTP ${response.code}")
            JSONObject(body)
        }
    }

    private fun parseOpenMeteoDashboard(json: JSONObject, config: WeatherApiConfig): WeatherDashboard {
        val basic = parseSnapshot(json, config)
        val currentJson = json.optJSONObject("current")
        val current = basic.copy(
            apparentTemperature = currentJson.number("apparent_temperature"),
            windDirection = currentJson.number("wind_direction_10m"),
            pressure = currentJson.number("surface_pressure"),
            visibility = currentJson.number("visibility")?.div(1000.0),
            observationTime = currentJson?.optString("time")?.takeIf(String::isNotBlank),
            solarElevation = calculateSolarElevation(
                time = currentJson?.optString("time").orEmpty(),
                latitude = config.latitude.toDoubleOrNull(),
                longitude = config.longitude.toDoubleOrNull(),
                utcOffsetSeconds = json.optInt("utc_offset_seconds", 0),
            ),
            timezone = json.optString("timezone").takeIf(String::isNotBlank),
        )

        val hourlyJson = json.optJSONObject("hourly")
        val hourly = if (hourlyJson == null) emptyList() else {
            val times = hourlyJson.optJSONArray("time")
            // 截断到整点再比较：current.time 带分钟（如 14:35）时逐字符串比较会跳过
            // 当前整点小时，逐小时条带便从「现在」直接跳到两小时后。
            val currentHour = currentJson?.optString("time").orEmpty().take(13)
            val start = (0 until (times?.length() ?: 0))
                .firstOrNull { times?.optString(it).orEmpty().take(13) >= currentHour }
                ?: 0
            (start until minOf(start + 24, times?.length() ?: 0)).mapNotNull { index ->
                val temperature = hourlyJson.arrayNumber("temperature_2m", index) ?: return@mapNotNull null
                HourlyForecast(
                    time = times?.optString(index).orEmpty(),
                    temperature = temperature,
                    condition = conditionText(hourlyJson.arrayValue("weather_code", index)),
                    precipitationProbability = hourlyJson.arrayNumber("precipitation_probability", index),
                    precipitationAmount = hourlyJson.arrayNumber("precipitation", index),
                    windSpeed = hourlyJson.arrayNumber("wind_speed_10m", index),
                    windDirection = hourlyJson.arrayNumber("wind_direction_10m", index),
                )
            }
        }

        val solarDay = hourlyJson?.optJSONArray("time")?.let { times ->
            val observationDate = currentJson?.optString("time").orEmpty().take(10)
            (0 until times.length()).mapNotNull { index ->
                val time = times.optString(index)
                if (!time.startsWith(observationDate)) return@mapNotNull null
                calculateSolarElevation(
                    time = time,
                    latitude = config.latitude.toDoubleOrNull(),
                    longitude = config.longitude.toDoubleOrNull(),
                    utcOffsetSeconds = json.optInt("utc_offset_seconds", 0),
                )?.let { SolarPosition(time, it) }
            }
        }.orEmpty()

        val dailyJson = json.optJSONObject("daily")
        val daily = if (dailyJson == null) emptyList() else {
            val dates = dailyJson.optJSONArray("time")
            (0 until minOf(16, dates?.length() ?: 0)).mapNotNull { index ->
                val high = dailyJson.arrayNumber("temperature_2m_max", index) ?: return@mapNotNull null
                val low = dailyJson.arrayNumber("temperature_2m_min", index) ?: return@mapNotNull null
                DailyForecast(
                    date = dates?.optString(index).orEmpty(),
                    high = high,
                    low = low,
                    condition = conditionText(dailyJson.arrayValue("weather_code", index)),
                    precipitationProbability = dailyJson.arrayNumber("precipitation_probability_max", index),
                    windSpeed = dailyJson.arrayNumber("wind_speed_10m_max", index),
                    windDirection = dailyJson.arrayNumber("wind_direction_10m_dominant", index),
                    uvIndex = dailyJson.arrayNumber("uv_index_max", index),
                    sunrise = dailyJson.arrayString("sunrise", index),
                    sunset = dailyJson.arrayString("sunset", index),
                )
            }
        }
        return WeatherDashboard(current = current, hourly = hourly, daily = daily, solarDay = solarDay)
    }

    private fun fetchOpenMeteoAirQuality(config: WeatherApiConfig): AirQuality? = runCatching {
        val standard = config.airQualityStandard.uppercase().let { if (it == "EU") "EU" else "US" }
        val aqiField = if (standard == "EU") "european_aqi" else "us_aqi"
        val url = (config.airQualityEndpoint.toHttpUrlOrNull() ?: return@runCatching null)
            .newBuilder()
            .setQueryParameter("latitude", config.latitude)
            .setQueryParameter("longitude", config.longitude)
            .setQueryParameter(
                "current",
                "$aqiField,pm2_5,pm10,nitrogen_dioxide,sulphur_dioxide,ozone",
            )
            .setQueryParameter("timezone", "auto")
            .build()
        val request = Request.Builder().url(url).header("User-Agent", "NekoWeather/0.1").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@runCatching null
            val current = JSONObject(response.body?.string().orEmpty()).optJSONObject("current")
                ?: return@runCatching null
            AirQuality(
                standard = standard,
                aqi = current.number(aqiField),
                pm25 = current.number("pm2_5"),
                pm10 = current.number("pm10"),
                nitrogenDioxide = current.number("nitrogen_dioxide"),
                sulphurDioxide = current.number("sulphur_dioxide"),
                ozone = current.number("ozone"),
            )
        }
    }.getOrNull()

    internal fun parseSnapshot(json: JSONObject, config: WeatherApiConfig): WeatherSnapshot {
        val temperature = JsonPathExtractor.number(json, config.temperaturePath)
            ?: throw IllegalArgumentException("未能从 ${config.temperaturePath} 读取温度")
        val rawCondition = JsonPathExtractor.get(json, config.conditionPath)
        return WeatherSnapshot(
            location = config.locationName.ifBlank { config.displayName },
            temperature = temperature,
            condition = conditionText(rawCondition),
            humidity = JsonPathExtractor.number(json, config.humidityPath),
            windSpeed = JsonPathExtractor.number(json, config.windSpeedPath),
        )
    }

    private fun parseQuery(query: String): List<Pair<String, String>> = query
        .trim().trimStart('?').split('&')
        .mapNotNull { part ->
            if (part.isBlank()) return@mapNotNull null
            val pieces = part.split('=', limit = 2)
            pieces.first().takeIf(String::isNotBlank)?.let { it to pieces.getOrElse(1) { "" } }
        }

    private fun conditionText(value: Any?): String {
        val code = when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
        return code?.let(::wmoDescription) ?: value?.toString()?.takeIf(String::isNotBlank) ?: "天气未知"
    }

    private fun wmoDescription(code: Int): String = when (code) {
        0 -> "晴朗"
        1 -> "大致晴朗"
        2 -> "局部多云"
        3 -> "阴天"
        45, 48 -> "有雾"
        51, 53, 55 -> "毛毛雨"
        56, 57 -> "冻毛毛雨"
        61, 63, 65 -> "下雨"
        66, 67 -> "冻雨"
        71, 73, 75, 77 -> "下雪"
        80, 81, 82 -> "阵雨"
        85, 86 -> "阵雪"
        95, 96, 99 -> "雷暴"
        else -> "天气代码 $code"
    }

    private fun JSONObject?.number(name: String): Double? {
        if (this == null || isNull(name)) return null
        return when (val value = opt(name)) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun JSONObject.arrayValue(name: String, index: Int): Any? =
        optJSONArray(name)?.opt(index)?.takeUnless { it == JSONObject.NULL }

    private fun JSONObject.arrayNumber(name: String, index: Int): Double? = when (val value = arrayValue(name, index)) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }

    private fun JSONObject.arrayString(name: String, index: Int): String? =
        optJSONArray(name)?.optString(index)?.takeIf(String::isNotBlank)

    private fun calculateSolarElevation(
        time: String,
        latitude: Double?,
        longitude: Double?,
        utcOffsetSeconds: Int,
    ): Double? = runCatching {
        if (latitude == null || longitude == null) return null
        val local = LocalDateTime.parse(time)
        val hour = local.hour + local.minute / 60.0
        val gamma = 2.0 * PI / 365.0 * (local.dayOfYear - 1 + (hour - 12.0) / 24.0)
        val equationOfTime = 229.18 * (
            0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma)
            )
        val declination = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) -
            0.006758 * cos(2 * gamma) + 0.000907 * sin(2 * gamma) -
            0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
        val timezoneHours = utcOffsetSeconds / 3600.0
        var trueSolarMinutes = local.hour * 60.0 + local.minute + equationOfTime + 4.0 * longitude - 60.0 * timezoneHours
        trueSolarMinutes = ((trueSolarMinutes % 1440.0) + 1440.0) % 1440.0
        val hourAngle = Math.toRadians(trueSolarMinutes / 4.0 - 180.0)
        val latitudeRadians = Math.toRadians(latitude)
        val cosZenith = (
            sin(latitudeRadians) * sin(declination) +
                cos(latitudeRadians) * cos(declination) * cos(hourAngle)
            ).coerceIn(-1.0, 1.0)
        90.0 - Math.toDegrees(acos(cosZenith))
    }.getOrNull()
}
