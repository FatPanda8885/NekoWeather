package com.neko.weather.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.neko.weather.model.AppLanguage

object LauncherLabelManager {
    fun labelFor(language: AppLanguage): String = when (language) {
        AppLanguage.SIMPLIFIED_CHINESE, AppLanguage.TRADITIONAL_CHINESE -> "猫猫风雨"
        AppLanguage.ENGLISH, AppLanguage.JAPANESE -> "NekoWeather"
    }

    fun sync(context: Context, language: AppLanguage) {
        val chinese = language == AppLanguage.SIMPLIFIED_CHINESE || language == AppLanguage.TRADITIONAL_CHINESE
        val packageManager = context.packageManager
        val chineseLauncher = ComponentName(context.packageName, "${context.packageName}.ChineseLauncher")
        val globalLauncher = ComponentName(context.packageName, "${context.packageName}.GlobalLauncher")
        val target = if (chinese) chineseLauncher else globalLauncher
        val obsolete = if (chinese) globalLauncher else chineseLauncher

        // Always enable the replacement before disabling the launcher that may have started
        // the current task. Some OEM launchers close the task if no enabled launcher exists,
        // even when DONT_KILL_APP is supplied.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && setEnabledAtomically(packageManager, target, obsolete)) {
            return
        }
        if (setEnabled(packageManager, target, enabled = true)) {
            setEnabled(packageManager, obsolete, enabled = false)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun setEnabledAtomically(
        packageManager: PackageManager,
        target: ComponentName,
        obsolete: ComponentName,
    ): Boolean = runCatching {
        packageManager.setComponentEnabledSettings(
            listOf(
                PackageManager.ComponentEnabledSetting(
                    target,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP,
                ),
                PackageManager.ComponentEnabledSetting(
                    obsolete,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                ),
            ),
        )
    }.onFailure { error ->
        Log.w("NekoWeather", "Unable to update launcher labels atomically", error)
    }.isSuccess

    private fun setEnabled(packageManager: PackageManager, component: ComponentName, enabled: Boolean): Boolean {
        val desired = if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        return runCatching {
            if (packageManager.getComponentEnabledSetting(component) != desired) {
                packageManager.setComponentEnabledSetting(component, desired, PackageManager.DONT_KILL_APP)
            }
        }.onFailure { error ->
            Log.w("NekoWeather", "Unable to update launcher label component ${component.className}", error)
        }.isSuccess
    }
}
