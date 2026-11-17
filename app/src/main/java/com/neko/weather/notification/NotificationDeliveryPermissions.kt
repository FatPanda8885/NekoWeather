package com.neko.weather.notification

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * 后台送达所需的两项系统授权。
 *
 * 注意这两项不是「锦上添花」而是硬门槛：
 * - Android 14 起 SCHEDULE_EXACT_ALARM 默认拒绝，没有它闹钟只能走不精确通道，可能晚几小时；
 * - 应用一旦被 ROM 判为受限，闹钟与广播都不会送达，必须由用户加入电池优化白名单。
 */
object NotificationDeliveryPermissions {
    fun canScheduleExactAlarms(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            runCatching { alarms(context).canScheduleExactAlarms() }.getOrDefault(false)

    fun isIgnoringBatteryOptimizations(context: Context): Boolean = runCatching {
        power(context).isIgnoringBatteryOptimizations(context.packageName)
    }.getOrDefault(false)

    fun requestExactAlarms(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, packageUri(context))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { openAppDetails(context) }
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri(context))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { openAppDetails(context) }
    }

    /** 应用详情页：ColorOS / MIUI 等的「自启动」「后台运行」开关通常在这一层。 */
    fun openAppDetails(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri(context))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    private fun packageUri(context: Context): Uri = Uri.parse("package:${context.packageName}")

    private fun alarms(context: Context): AlarmManager = context.getSystemService(AlarmManager::class.java)

    private fun power(context: Context): PowerManager = context.getSystemService(PowerManager::class.java)
}
