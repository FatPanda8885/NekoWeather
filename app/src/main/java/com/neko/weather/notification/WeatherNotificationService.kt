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
import com.neko.weather.data.AppTimeZone
import com.neko.weather.data.ConfigStore
import com.neko.weather.data.WeatherRepository
import com.neko.weather.data.WeatherCacheStore
import com.neko.weather.model.AppLanguage
import com.neko.weather.model.DailyForecast
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
import java.time.temporal.TemporalAdjusters
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
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val language = ConfigStore(context).loadDisplaySettings().language
        listOf(
            NotificationChannel("forecast", localizedText("天气提醒", language), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel("rain", localizedText("降雨提醒", language), NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel("alerts", localizedText("气象灾害预警", language), NotificationManager.IMPORTANCE_HIGH),
        ).forEach(manager::createNotificationChannel)
    }
}

/**
 * 通知发布器：后台 Job 与前台天气刷新共用同一套判定与去重逻辑。
 * 部分 ROM 会大幅推迟甚至省略周期 Job，前台刷新路径保证用户使用期间通知照常到期。
 */
object WeatherNotificationPublisher {
    fun publish(context: Context, weather: WeatherDashboard) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        WeatherNotificationScheduler.createChannels(context)
        val store = ConfigStore(context)
        val settings = store.loadNotificationSettings()
        if (!settings.morningEnabled && !settings.weeklyEnabled && !settings.rainEnabled && !settings.alertEnabled) return
        val display = store.loadDisplaySettings()
        val zone = AppTimeZone.resolve(display.timeZoneId)
        val unit = display.temperatureUnit
        fun l(value: String) = localizedText(value, display.language)
        val prefs = context.getSharedPreferences("notification_history", Context.MODE_PRIVATE)
        val now = LocalDateTime.now(zone)
        val today = LocalDate.now(zone).toString()

        if (settings.morningEnabled && now.hour in 6..10 && prefs.getString("morning", "") != today) {
            val current = weather.current
            val todayForecast = weather.daily.getOrNull(1) ?: weather.daily.firstOrNull()
            notify(
                context, 1001, "forecast", l("${current.location}今日天气"),
                l("${current.condition} ${formatTemperature(current.temperature, unit)}，${todayForecast?.let { "${formatTemperature(it.low, unit)}～${formatTemperature(it.high, unit)}" }.orEmpty()}"),
            )
            prefs.edit().putString("morning", today).apply()
        }

        if (settings.weeklyEnabled && now.dayOfWeek == DayOfWeek.SUNDAY && now.hour in 8..14 && prefs.getString("weekly", "") != today) {
            weeklyOverview(weather, now.toLocalDate())?.let { summary ->
                notify(context, 1002, "forecast", l("下周天气速览"), l(summary))
                prefs.edit().putString("weekly", today).apply()
            }
        }

        if (settings.rainEnabled) {
            // 判据是逐小时预报的天气状况本身是否属于降水（雨/雪/雹/雷），不再看降水概率
            val wet = weather.hourly.take(4).firstOrNull { isPrecipitation(it.condition) }
            val wetKey = wet?.time.orEmpty()
            if (wet != null && prefs.getString("rain", "") != wetKey) {
                notify(
                    context, 1003, "rain",
                    l(precipitationTitle(wet.condition)),
                    // 正文按语言整句成模板，不再交给替换表拼装（见 precipitationBody 注释）
                    precipitationBody(display.language, wet.time.takeLast(5), wet.condition) { l(it) },
                )
                prefs.edit().putString("rain", wetKey).apply()
            }
        }

        if (settings.alertEnabled && weather.alerts.isNotEmpty()) {
            val key = weather.alerts.joinToString("|") { it.title + it.expiresAt }
            if (prefs.getString("alerts", "") != key) {
                val first = weather.alerts.first()
                notify(context, 1004, "alerts", "${first.severity} · ${first.title}", first.description.take(240))
                prefs.edit().putString("alerts", key).apply()
            }
        }
    }

    /**
     * 下周概览：以哪种天气为主 + 气温相比本周怎么走。
     *
     * 只有未来预报、没有历史数据，而这条通知在周日发，所以「本周」用今天到本周日的
     * 剩余预报天做基准；样本不足 2 天（周日当天就是这种情况）时退回与今天比较。
     */
    internal fun weeklyOverview(weather: WeatherDashboard, today: LocalDate): String? {
        val nextWeek = weather.daily.drop(1).take(7)
        if (nextWeek.isEmpty()) return null
        val dominant = nextWeek.groupingBy { it.condition }.eachCount()
            .entries.maxByOrNull { it.value }?.key
            .orEmpty().ifBlank { nextWeek.first().condition }

        val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val thisWeekRest = weather.daily.filter { day ->
            val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@filter false
            !date.isBefore(today) && !date.isAfter(weekEnd)
        }
        val baseline = thisWeekRest.takeIf { it.size >= 2 } ?: weather.daily.take(1)
        if (baseline.isEmpty()) return "下周以${dominant}为主。"

        val delta = meanTemperature(nextWeek) - meanTemperature(baseline)
        val trend = when {
            delta >= 1.5 -> "，气温较本周上升"
            delta <= -1.5 -> "，气温较本周下降"
            else -> "，气温与本周基本持平"
        }
        return "下周以${dominant}为主$trend。"
    }

    private fun meanTemperature(days: List<DailyForecast>): Double =
        days.map { (it.high + it.low) / 2 }.average()

    /** 天气状况是否属于降水（雨、雪、冰雹、雷暴都算，含雨夹雪/阵雪/冻雨等组合）。 */
    internal fun isPrecipitation(condition: String): Boolean =
        listOf("雨", "雪", "雹", "雷").any { it in condition }

    private fun precipitationTitle(condition: String): String = when {
        "雪" in condition -> "即将降雪"
        "雹" in condition -> "即将有冰雹"
        "雷" in condition -> "即将有雷雨"
        else -> "即将降雨"
    }

    /**
     * 降水正文按语言整句成句。
     *
     * 本地化表是逐串替换，无法重排语序；若只把天气类型和建议交给表翻译，英文界面会得到
     * 「预计 23:00 前后Snow showers，Roads may be slippery…」这类中英混排。
     * 所以这里按语言给出完整模板，只让 [localize] 负责其中的词与句。
     */
    internal fun precipitationBody(
        language: AppLanguage,
        time: String,
        condition: String,
        localize: (String) -> String,
    ): String {
        val kind = localize(condition)
        val advice = localize(
            when {
                "雪" in condition -> "路面可能湿滑，注意保暖与出行安全。"
                "雹" in condition || "雷" in condition -> "尽量留在室内，注意安全。"
                else -> "外出请带伞。"
            },
        )
        return when (language) {
            AppLanguage.ENGLISH -> "Expect $kind around $time. $advice"
            AppLanguage.JAPANESE -> "$time 頃に$kind。$advice"
            AppLanguage.SIMPLIFIED_CHINESE -> "预计 $time 前后$kind，$advice"
            AppLanguage.TRADITIONAL_CHINESE -> localize("预计 $time 前后$kind，$advice")
        }
    }

    /** 通知推送测试（仅 dev 构建入口可见）。 */
    fun test(context: Context) {
        WeatherNotificationScheduler.createChannels(context)
        val language = ConfigStore(context).loadDisplaySettings().language
        notify(
            context, 1005, "forecast",
            localizedText("通知测试", language),
            localizedText("看到这条通知，说明通知通道工作正常。", language),
        )
    }

    private fun notify(context: Context, id: Int, channel: String, title: String, text: String) {
        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    private fun formatTemperature(celsius: Double, unit: TemperatureUnit): String = when (unit) {
        TemperatureUnit.CELSIUS -> "${celsius.roundToInt()}°C"
        TemperatureUnit.FAHRENHEIT -> "${(celsius * 9 / 5 + 32).roundToInt()}°F"
        TemperatureUnit.KELVIN -> "${(celsius + 273.15).roundToInt()} K"
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
                    // 数据还新鲜就不重复请求，但通知判定要用缓存再跑一遍：
                    // 否则周期 Job 每次都落在这个窗口里，等于整天不推任何通知
                    WeatherCacheStore(applicationContext).load(store.loadWeatherConfig())
                        ?.let { WeatherNotificationPublisher.publish(applicationContext, it) }
                    return@runCatching
                }
                val dashboard = WeatherRepository().fetch(store.loadWeatherConfig())
                store.saveLastWeatherUpdateMillis(dashboard.current.fetchedAtMillis)
                WeatherNotificationPublisher.publish(applicationContext, dashboard)
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
}
