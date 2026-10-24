package com.neko.weather.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.neko.weather.MainActivity
import com.neko.weather.data.ConfigStore
import com.neko.weather.data.WeatherRepository
import com.neko.weather.model.WeatherDashboard
import com.neko.weather.model.TemperatureUnit
import com.neko.weather.ui.localizedText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.roundToInt

object WeatherNotificationScheduler {
    private const val JOB_ID = 48102

    fun sync(context: Context): Boolean = runCatching {
        createChannels(context)
        val settings = ConfigStore(context).loadNotificationSettings()
        val scheduler = context.getSystemService(JobScheduler::class.java)
        if (!settings.morningEnabled && !settings.weeklyEnabled && !settings.rainEnabled && !settings.alertEnabled) {
            scheduler.cancel(JOB_ID)
            return@runCatching true
        }
        // 部分厂商 ROM 对 setPeriodic(period, flex) 的 flex 校验与 AOSP 不一致。
        // 使用单参数版本，由系统自行批处理，避免任务注册影响应用启动。
        val job = JobInfo.Builder(JOB_ID, ComponentName(context, WeatherNotificationJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .setPeriodic(30 * 60 * 1000L)
            .build()
        scheduler.schedule(job) == JobScheduler.RESULT_SUCCESS
    }.getOrElse { error ->
        Log.e("NekoWeather", "Unable to schedule weather notifications", error)
        false
    }

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val language = ConfigStore(context).loadDisplaySettings().language
        listOf(
            NotificationChannel("forecast", localizedText("天气提醒", language), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel("rain", localizedText("降雨提醒", language), NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel("alerts", localizedText("气象灾害预警", language), NotificationManager.IMPORTANCE_HIGH),
        ).forEach(manager::createNotificationChannel)
    }
}

class WeatherNotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) {
            WeatherNotificationScheduler.sync(context.applicationContext)
        }
    }
}

class WeatherNotificationJobService : JobService() {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            runCatching {
                val store = ConfigStore(applicationContext)
                val refresh = store.loadWeatherRefreshSettings()
                val age = System.currentTimeMillis() - store.loadLastWeatherUpdateMillis()
                if (refresh.freshnessGuardEnabled && age in 0 until refresh.freshnessMinutes * 60_000L) {
                    return@runCatching
                }
                val dashboard = WeatherRepository().fetch(store.loadWeatherConfig())
                store.saveLastWeatherUpdateMillis(dashboard.current.fetchedAtMillis)
                publishEnabledNotifications(store, dashboard)
            }
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun publishEnabledNotifications(store: ConfigStore, weather: WeatherDashboard) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val settings = store.loadNotificationSettings()
        val display = store.loadDisplaySettings()
        val unit = display.temperatureUnit
        fun l(value: String) = localizedText(value, display.language)
        val prefs = getSharedPreferences("notification_history", MODE_PRIVATE)
        val now = LocalDateTime.now()
        val today = LocalDate.now().toString()

        if (settings.morningEnabled && now.hour in 6..10 && prefs.getString("morning", "") != today) {
            val current = weather.current
            val todayForecast = weather.daily.getOrNull(1) ?: weather.daily.firstOrNull()
            notify(
                1001, "forecast", l("${current.location}今日天气"),
                l("${current.condition} ${formatTemperature(current.temperature, unit)}，${todayForecast?.let { "${formatTemperature(it.low, unit)}～${formatTemperature(it.high, unit)}" }.orEmpty()}"),
            )
            prefs.edit().putString("morning", today).apply()
        }

        if (settings.weeklyEnabled && now.dayOfWeek == DayOfWeek.SUNDAY && now.hour in 8..14 && prefs.getString("weekly", "") != today) {
            val summary = weather.daily.drop(1).take(7).joinToString("；") { "${it.date.takeLast(5)} ${it.condition} ${formatTemperature(it.low, unit)}～${formatTemperature(it.high, unit)}" }
            notify(1002, "forecast", l("下周天气速览"), l(summary))
            prefs.edit().putString("weekly", today).apply()
        }

        if (settings.rainEnabled) {
            val rain = weather.hourly.take(4).firstOrNull { (it.precipitationProbability ?: 0.0) >= 50.0 }
            val rainKey = rain?.time.orEmpty()
            if (rain != null && prefs.getString("rain", "") != rainKey) {
                notify(1003, "rain", l("即将降雨"), l("${rain.time.takeLast(5)} 前后降雨概率 ${rain.precipitationProbability?.roundToInt()}%，外出请带伞。"))
                prefs.edit().putString("rain", rainKey).apply()
            }
        }

        if (settings.alertEnabled && weather.alerts.isNotEmpty()) {
            val key = weather.alerts.joinToString("|") { it.title + it.expiresAt }
            if (prefs.getString("alerts", "") != key) {
                val first = weather.alerts.first()
                notify(1004, "alerts", "${first.severity} · ${first.title}", first.description.take(240))
                prefs.edit().putString("alerts", key).apply()
            }
        }
    }

    private fun notify(id: Int, channel: String, title: String, text: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    private fun formatTemperature(celsius: Double, unit: TemperatureUnit): String = when (unit) {
        TemperatureUnit.CELSIUS -> "${celsius.roundToInt()}°C"
        TemperatureUnit.FAHRENHEIT -> "${(celsius * 9 / 5 + 32).roundToInt()}°F"
        TemperatureUnit.KELVIN -> "${(celsius + 273.15).roundToInt()} K"
    }
}
