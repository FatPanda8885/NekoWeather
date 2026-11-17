package com.neko.weather.notification

import android.Manifest
import android.app.AlarmManager
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
import com.neko.weather.model.WeatherAlert
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
import java.time.OffsetDateTime
import kotlin.math.roundToInt

object WeatherNotificationScheduler {
    private const val PERIODIC_JOB_ID = 48102
    private const val ONESHOT_JOB_ID = 48103
    private const val ALARM_CHECK = 48201
    private const val ALARM_TEST = 48202

    const val CHANNEL_KEEP_ALIVE = "keepalive"

    internal const val ACTION_CHECK = "com.neko.weather.action.NOTIFICATION_CHECK"
    internal const val ACTION_TEST = "com.neko.weather.action.NOTIFICATION_TEST"

    /** 例行检查间隔：降雨看未来 4 小时、预警随时可能更新，一小时一次足够且不费电。 */
    internal const val CHECK_INTERVAL_MINUTES = 60L

    private const val NEXT_CHECK_KEY = "nextCheckAt"
    private const val NEXT_CHECK_EXACT_KEY = "nextCheckExact"

    fun sync(context: Context): Boolean = runCatching {
        createChannels(context)
        val settings = ConfigStore(context).loadNotificationSettings()
        val enabled = settings.morningEnabled || settings.weeklyEnabled || settings.rainEnabled || settings.alertEnabled
        if (!enabled) {
            cancelAll(context)
            return@runCatching true
        }
        // 两条腿走路：周期 Job 兜底，闹钟负责准时。不同 ROM 对二者的态度并不一致，
        // 只留一条都容易全灭（此前只有周期 Job，Doze 下会被推迟数小时）。
        schedulePeriodicJob(context)
        scheduleNextCheck(context)
        NotificationKeepAlive.sync(context)
        true
    }.getOrElse { error ->
        Log.e("NekoWeather", "Unable to schedule weather notifications", error)
        false
    }

    fun cancelAll(context: Context) {
        context.getSharedPreferences("notification_history", Context.MODE_PRIVATE)
            .edit().remove(NEXT_CHECK_KEY).apply()
        val jobs = context.getSystemService(JobScheduler::class.java)
        runCatching { jobs.cancel(PERIODIC_JOB_ID) }
        runCatching { jobs.cancel(ONESHOT_JOB_ID) }
        runCatching { alarms(context).cancel(alarmIntent(context, ALARM_CHECK, ACTION_CHECK)) }
        runCatching { alarms(context).cancel(alarmIntent(context, ALARM_TEST, ACTION_TEST)) }
    }

    private fun schedulePeriodicJob(context: Context) {
        // 部分厂商 ROM 对 setPeriodic(period, flex) 的 flex 校验与 AOSP 不一致。
        // 使用单参数版本，由系统自行批处理，避免任务注册影响应用启动。
        val job = JobInfo.Builder(PERIODIC_JOB_ID, ComponentName(context, WeatherNotificationJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .setPeriodic(30 * 60 * 1000L)
            .build()
        runCatching { context.getSystemService(JobScheduler::class.java).schedule(job) }
    }

    /**
     * 排下一次例行检查。
     *
     * 有精确闹钟权限时用 setExactAndAllowWhileIdle（Doze 也能准点唤醒）；
     * 没权限（Android 14 起默认拒绝）时退化为 setAndAllowWhileIdle，仍会送达但可能延后。
     */
    fun scheduleNextCheck(context: Context): Boolean = runCatching {
        val prefs = context.getSharedPreferences("notification_history", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val exact = NotificationDeliveryPermissions.canScheduleExactAlarms(context)
        val armedAt = prefs.getLong(NEXT_CHECK_KEY, 0L)
        val armedExact = prefs.getBoolean(NEXT_CHECK_EXACT_KEY, false)
        // 同一个 PendingIntent 再 set 一次是「替换」而不是「新增」：如果每次打开应用都重排，
        // 闹钟会被无限往后推、永远不会触发。已排且授权状态没变就什么都不做。
        if (armedAt > now && armedExact == exact) return@runCatching true
        // 授权状态变了要重排，但保留原来的触发时刻，不要借此把闹钟往后推
        val trigger = if (armedAt > now) armedAt else now + CHECK_INTERVAL_MINUTES * 60_000L
        val armed = armAlarm(context, trigger, ALARM_CHECK, ACTION_CHECK)
        if (armed) prefs.edit().putLong(NEXT_CHECK_KEY, trigger).putBoolean(NEXT_CHECK_EXACT_KEY, exact).apply()
        armed
    }.getOrElse { error ->
        Log.e("NekoWeather", "Unable to arm notification alarm", error)
        false
    }

    /** 广播里不做网络请求：排一个一次性 Job，真正抓数据仍在 JobService 里完成。 */
    fun enqueueCheckNow(context: Context) {
        val builder = JobInfo
            .Builder(ONESHOT_JOB_ID, ComponentName(context, WeatherNotificationJobService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setExpedited(true)
        } else {
            builder.setOverrideDeadline(0)
        }
        runCatching { context.getSystemService(JobScheduler::class.java).schedule(builder.build()) }
    }

    /**
     * 调试用：延迟若干秒后发一条测试通知，用来验证「应用不在前台」时能否送达。
     *
     * 返回 true 表示用的是精确闹钟；false 表示系统不允许精确闹钟（Android 12+ 可被用户收回），
     * 已退化为 setAndAllowWhileIdle，可能晚几分钟才到。
     */
    fun scheduleTestNotification(context: Context, delaySeconds: Long): Boolean {
        createChannels(context)
        val trigger = System.currentTimeMillis() + delaySeconds.coerceIn(1L, 3600L) * 1000L
        val exact = NotificationDeliveryPermissions.canScheduleExactAlarms(context)
        armAlarm(context, trigger, ALARM_TEST, ACTION_TEST)
        return exact
    }

    private fun alarms(context: Context): AlarmManager = context.getSystemService(AlarmManager::class.java)

    /**
     * 排一个「必须送达」的闹钟。
     *
     * 有精确闹钟权限时用 setAlarmClock：系统把它当作用户可见的闹钟，不受 Doze、App Standby
     * 与多数厂商后台管控影响，是国产 ROM 上本地定时通知唯一稳定的通道。代价是状态栏会显示闹钟图标。
     * 没有权限时退化为 setAndAllowWhileIdle（会被推迟，但至少不是完全收不到）。
     */
    private fun armAlarm(context: Context, triggerAtMillis: Long, requestCode: Int, action: String): Boolean =
        runCatching {
            val manager = alarms(context)
            val pending = alarmIntent(context, requestCode, action)
            if (NotificationDeliveryPermissions.canScheduleExactAlarms(context)) {
                manager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, null), pending)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
            true
        }.getOrElse { error ->
            Log.e("NekoWeather", "Unable to arm alarm $action", error)
            false
        }

    private fun alarmIntent(context: Context, requestCode: Int, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, WeatherNotificationAlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val language = ConfigStore(context).loadDisplaySettings().language
        listOf(
            NotificationChannel("forecast", localizedText("天气提醒", language), NotificationManager.IMPORTANCE_DEFAULT),
            NotificationChannel("rain", localizedText("降雨提醒", language), NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel("alerts", localizedText("气象灾害预警", language), NotificationManager.IMPORTANCE_HIGH),
            NotificationChannel(CHANNEL_KEEP_ALIVE, localizedText("后台运行", language), NotificationManager.IMPORTANCE_MIN),
        ).forEach(manager::createNotificationChannel)
    }
}

/**
 * 通知发布器：后台 Job 与前台天气刷新共用同一套判定与去重逻辑。
 * 部分 ROM 会大幅推迟甚至省略周期 Job，前台刷新路径保证用户使用期间通知照常到期。
 */
object WeatherNotificationPublisher {
    /** 超过这个时长的天气数据一律不据此发通知，避免缓存数据推出早已过去的时段。 */
    private const val DEFAULT_MAX_DATA_AGE_MINUTES = 360L

    fun publish(
        context: Context,
        weather: WeatherDashboard,
        maxDataAgeMinutes: Long = DEFAULT_MAX_DATA_AGE_MINUTES,
    ) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            NotificationDiagnostics.recordPublish(context, "跳过：没有通知权限")
            return
        }
        WeatherNotificationScheduler.createChannels(context)
        val store = ConfigStore(context)
        val settings = store.loadNotificationSettings()
        if (!settings.morningEnabled && !settings.weeklyEnabled && !settings.rainEnabled && !settings.alertEnabled) {
            NotificationDiagnostics.recordPublish(context, "跳过：通知开关全关")
            return
        }

        // 数据太旧就什么都不发：周期任务被推迟数小时后用缓存数据判定，会推出已经过去的时间段。
        val fetchedAt = weather.current.fetchedAtMillis
        if (fetchedAt > 0L) {
            val ageMinutes = (System.currentTimeMillis() - fetchedAt) / 60_000L
            if (ageMinutes > maxDataAgeMinutes) {
                Log.i("NekoWeather", "Skip notifications: weather data is $ageMinutes min old")
                NotificationDiagnostics.recordPublish(context, "跳过：天气数据已过期 $ageMinutes 分钟")
                return
            }
        }
        val display = store.loadDisplaySettings()
        val zone = AppTimeZone.resolve(display.timeZoneId)
        val unit = display.temperatureUnit
        fun l(value: String) = localizedText(value, display.language)
        val prefs = context.getSharedPreferences("notification_history", Context.MODE_PRIVATE)
        val now = LocalDateTime.now(zone)
        val today = LocalDate.now(zone).toString()
        var sent = 0

        if (settings.morningEnabled && now.hour in 6..10 && prefs.getString("morning", "") != today) {
            val current = weather.current
            val todayForecast = dailyFor(weather, now.toLocalDate())
            // 没有今天的高低温时不要留悬空逗号
            val range = todayForecast
                ?.let { "，${formatTemperature(it.low, unit)}～${formatTemperature(it.high, unit)}" }
                .orEmpty()
            notify(
                context, 1001, "forecast", l("${current.location}今日天气"),
                l("${current.condition} ${formatTemperature(current.temperature, unit)}$range"),
            )
            prefs.edit().putString("morning", today).apply()
            sent++
        }

        if (settings.weeklyEnabled && now.dayOfWeek == DayOfWeek.SUNDAY && now.hour in 8..14 && prefs.getString("weekly", "") != today) {
            weeklyOverview(weather, now.toLocalDate())?.let { summary ->
                notify(context, 1002, "forecast", l("下周天气速览"), l(summary))
                prefs.edit().putString("weekly", today).apply()
                sent++
            }
        }

        if (settings.rainEnabled) {
            // 判据是逐小时预报的天气状况本身是否属于降水（雨/雪/雹/雷），不再看降水概率；
            // 同时只取尚未开始的整点，否则缓存里的旧时段会被当成「即将降雨」推出来。
            val wet = weather.hourly
                .filter { isUpcomingHour(it.time, now) }
                .take(4)
                .firstOrNull { isPrecipitation(it.condition) }
            val wetKey = wet?.time.orEmpty()
            if (wet != null && prefs.getString("rain", "") != wetKey) {
                notify(
                    context, 1003, "rain",
                    l(precipitationTitle(wet.condition)),
                    // 正文按语言整句成模板，不再交给替换表拼装（见 precipitationBody 注释）
                    precipitationBody(display.language, wet.time.takeLast(5), wet.condition) { l(it) },
                )
                prefs.edit().putString("rain", wetKey).apply()
                sent++
            }
        }

        if (settings.alertEnabled) {
            // 过期预警不再推送：打开应用时一次性补发的「过时通知」主要来自这里
            val active = weather.alerts.filterNot { isExpired(it.expiresAt, now) }
            if (active.isNotEmpty()) {
                val key = active.joinToString("|") { it.title + it.expiresAt }
                if (prefs.getString("alerts", "") != key) {
                    val first = active.first()
                    notify(context, 1004, "alerts", "${first.severity} · ${first.title}", first.description.take(240))
                    prefs.edit().putString("alerts", key).apply()
                    sent++
                }
            }
        }

        NotificationDiagnostics.recordPublish(
            context,
            if (sent > 0) "已发送 $sent 条通知" else "检查完成，无到期通知",
        )
    }

    /**
     * 下周概览：以哪种天气为主 + 气温相比本周怎么走。
     *
     * 只有未来预报、没有历史数据，而这条通知在周日发，所以「本周」用今天到本周日的
     * 剩余预报天做基准；样本不足 2 天（周日当天就是这种情况）时退回与今天比较。
     */
    /**
     * 取指定那天的预报。
     *
     * Open-Meteo 默认请求带 past_days=1（daily[0] 是昨天），QWeather 从今天开始（daily[0] 是今天），
     * 数据起点不一致，用数组下标必然有一个供应商取错 —— 只能按日期找。
     * 日期全部解析不了时退回数组首项，保持函数总有返回。
     */
    /** 该整点是否还没过去（含当前小时）；时间解析不了时按可用处理，避免误杀。 */
    internal fun isUpcomingHour(time: String, now: LocalDateTime): Boolean {
        val parsed = runCatching { LocalDateTime.parse(time) }.getOrNull() ?: return true
        return !parsed.isBefore(now.withMinute(0).withSecond(0).withNano(0))
    }

    /**
     * 预警是否已过期。expiresAt 有多种来源：CAP 的 ISO 带偏移串、本地日期时间、纯日期。
     * 缺失或无法解析时视为未过期（宁可多推一次，也不要吞掉真实预警）。
     */
    internal fun isExpired(expiresAt: String?, now: LocalDateTime): Boolean {
        val raw = expiresAt?.takeIf { it.isNotBlank() } ?: return false
        val parsed = runCatching { OffsetDateTime.parse(raw).toLocalDateTime() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(raw) }.getOrNull()
            ?: runCatching { LocalDate.parse(raw).atTime(23, 59) }.getOrNull()
            ?: return false
        return parsed.isBefore(now)
    }

    internal fun dailyFor(weather: WeatherDashboard, day: LocalDate): DailyForecast? {
        val dated = weather.daily.mapNotNull { forecast ->
            runCatching { LocalDate.parse(forecast.date) }.getOrNull()?.let { it to forecast }
        }
        if (dated.isEmpty()) return weather.daily.firstOrNull()
        return dated.firstOrNull { (date, _) -> date == day }?.second
    }

    internal fun weeklyOverview(weather: WeatherDashboard, today: LocalDate): String? {
        // 一切按日期取，不按数组下标：Open-Meteo 默认请求带 past_days=1，daily[0] 是昨天。
        // 日期解析不了的供应商退化为「数组首项是今天」，不影响两个内置供应商（都是 yyyy-MM-dd）。
        val dated = weather.daily.mapIndexed { index, day ->
            (runCatching { LocalDate.parse(day.date) }.getOrNull() ?: today.plusDays(index.toLong())) to day
        }
        val nextWeek = dated.filter { (date, _) -> date.isAfter(today) }.take(7).map { it.second }
        if (nextWeek.isEmpty()) return null
        val dominant = nextWeek.groupingBy { it.condition }.eachCount()
            .entries.maxByOrNull { it.value }?.key
            .orEmpty().ifBlank { nextWeek.first().condition }

        val weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val thisWeekRest = dated
            .filter { (date, _) -> !date.isBefore(today) && !date.isAfter(weekEnd) }
            .map { it.second }
        // 周日当天「本周剩余」只剩今天，退回与今天比较；今天缺失时用第一个未来日兜底
        val baseline = thisWeekRest.takeIf { it.size >= 2 }
            ?: dated.firstOrNull { (date, _) -> date == today }?.second?.let { listOf(it) }
            ?: nextWeek.take(1)

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

/** 闹钟入口：例行检查排一次性 Job；测试通知直接发。 */
class WeatherNotificationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        when (intent?.action) {
            WeatherNotificationScheduler.ACTION_TEST -> WeatherNotificationPublisher.test(app)
            WeatherNotificationScheduler.ACTION_CHECK -> {
                NotificationDiagnostics.recordFire(app)
                WeatherNotificationScheduler.enqueueCheckNow(app)
                WeatherNotificationScheduler.scheduleNextCheck(app)
            }
        }
    }
}

class WeatherNotificationBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) {
            WeatherNotificationScheduler.sync(context.applicationContext)
        }
    }
}

/** 抓一次天气并做通知判定，供 JobService 与前台保活服务共用。 */
internal object WeatherNotificationWork {
    suspend fun runOnce(context: Context) {
        runCatching {
            val store = ConfigStore(context)
            val refresh = store.loadWeatherRefreshSettings()
            val age = System.currentTimeMillis() - store.loadLastWeatherUpdateMillis()
            if (refresh.freshnessGuardEnabled && age in 0 until refresh.freshnessMinutes * 60_000L) {
                // 数据还新鲜就不重复请求，但通知判定要用缓存再跑一遍：
                // 否则每次检查都落在这个窗口里，等于整天不推任何通知
                WeatherCacheStore(context).load(store.loadWeatherConfig())
                    ?.let { cached ->
                        WeatherNotificationPublisher.publish(
                            context,
                            cached,
                            refresh.freshnessMinutes.coerceAtLeast(1).toLong(),
                        )
                    }
                return@runCatching
            }
            val dashboard = WeatherRepository().fetch(store.loadWeatherConfig())
            store.saveLastWeatherUpdateMillis(dashboard.current.fetchedAtMillis)
            WeatherNotificationPublisher.publish(context, dashboard)
        }.onFailure {
            NotificationDiagnostics.recordPublish(context, "检查失败：${it.message ?: it::class.java.simpleName}")
        }
    }
}

class WeatherNotificationJobService : JobService() {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            WeatherNotificationWork.runOnce(applicationContext)
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
