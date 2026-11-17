package com.neko.weather.notification

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.neko.weather.MainActivity
import com.neko.weather.data.ConfigStore
import com.neko.weather.ui.localizedText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 常驻保活：前台服务里自己跑检查循环。
 *
 * 国产 ROM（ColorOS / MIUI 等）会冻结后台进程，闹钟到点也无法唤醒，
 * 这是本地定时通知在这些机型上唯一稳定的落地方式。代价是通知栏常驻一条低优先级通知。
 */
object NotificationKeepAlive {
    private const val PREFS = "notification_history"
    private const val KEY_ENABLED = "keepAliveEnabled"

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
        sync(context)
    }

    /** 按当前开关状态启停服务；幂等，可在应用启动 / 设置变更 / 开机后重复调用。 */
    fun sync(context: Context) {
        if (isEnabled(context)) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, NotificationKeepAliveService::class.java),
                )
            }
        } else {
            runCatching { context.stopService(Intent(context, NotificationKeepAliveService::class.java)) }
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

class NotificationKeepAliveService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null

    override fun onCreate() {
        super.onCreate()
        WeatherNotificationScheduler.createChannels(this)
        // Android 12+ 禁止从后台启动前台服务；从开机广播进来时会在这里抛异常。
        // 捕获后直接停掉自己，避免反复崩溃。
        val started = runCatching { startForeground(FOREGROUND_ID, ongoingNotification()) }.isSuccess
        if (!started) {
            stopSelf()
            return
        }
        loop = scope.launch {
            while (isActive) {
                WeatherNotificationWork.runOnce(applicationContext)
                delay(CHECK_INTERVAL_MILLIS)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        loop?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ongoingNotification(): Notification {
        val language = ConfigStore(this).loadDisplaySettings().language
        val intent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, WeatherNotificationScheduler.CHANNEL_KEEP_ALIVE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(localizedText("后台提醒运行中", language))
            .setContentText(localizedText("保持每日天气与降雨提醒准时送达", language))
            .setContentIntent(intent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private companion object {
        const val FOREGROUND_ID = 1000
        const val CHECK_INTERVAL_MILLIS = 30 * 60 * 1000L
    }
}
