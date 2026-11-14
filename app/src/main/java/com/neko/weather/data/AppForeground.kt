package com.neko.weather.data

import java.util.concurrent.atomic.AtomicInteger

/** 记录 App 是否处于前台：Android 10+ 禁止应用在后台直接 startActivity 拉起安装器。 */
object AppForeground {
    private val startedActivities = AtomicInteger(0)

    val isForeground: Boolean get() = startedActivities.get() > 0

    fun onActivityStarted() { startedActivities.incrementAndGet() }

    fun onActivityStopped() { startedActivities.updateAndGet { current -> (current - 1).coerceAtLeast(0) } }
}
