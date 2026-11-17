package com.neko.weather.notification

import android.content.Context

/**
 * 记录后台通知链路的两个关键时间点，用于在设备上定位「到底断在哪一环」：
 * - [recordFire] 由闹钟接收器写，没更新说明闹钟根本没被系统触发；
 * - [recordPublish] 由发布器写，没更新说明闹钟触发了但执行失败。
 */
object NotificationDiagnostics {
    private const val PREFS = "notification_history"
    private const val KEY_LAST_FIRE = "diagLastFireAt"
    private const val KEY_LAST_PUBLISH = "diagLastPublishAt"
    private const val KEY_LAST_RESULT = "diagLastResult"

    fun recordFire(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_FIRE, System.currentTimeMillis()).apply()
    }

    fun recordPublish(context: Context, result: String) {
        prefs(context).edit()
            .putLong(KEY_LAST_PUBLISH, System.currentTimeMillis())
            .putString(KEY_LAST_RESULT, result)
            .apply()
    }

    fun lastFireAt(context: Context): Long = prefs(context).getLong(KEY_LAST_FIRE, 0L)

    fun lastPublishAt(context: Context): Long = prefs(context).getLong(KEY_LAST_PUBLISH, 0L)

    fun lastResult(context: Context): String = prefs(context).getString(KEY_LAST_RESULT, "").orEmpty()

    fun nextCheckAt(context: Context): Long =
        prefs(context).getLong("nextCheckAt", 0L)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
