package com.neko.weather.data

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupCategories {
    const val SETTINGS = "settings"
    const val APPEARANCE = "appearance"
    const val API_USAGE = "api_usage"
    const val NOTIFICATION_HISTORY = "notification_history"
    val all = setOf(SETTINGS, APPEARANCE, API_USAGE, NOTIFICATION_HISTORY)
}

class BackupManager(private val context: Context) {
    fun backup(uri: Uri, categories: Set<String>) {
        context.contentResolver.openOutputStream(uri, "w")?.use { raw ->
            ZipOutputStream(raw.buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(JSONObject().put("format", "NekoWeatherBackup").put("version", 1).put("categories", org.json.JSONArray(categories.toList())).toString(2).toByteArray())
                zip.closeEntry()
                if (BackupCategories.SETTINGS in categories) writePreferences(zip, "neko_weather")
                if (BackupCategories.API_USAGE in categories) writePreferences(zip, "api_usage")
                if (BackupCategories.NOTIFICATION_HISTORY in categories) writePreferences(zip, "notification_history")
                if (BackupCategories.APPEARANCE in categories) {
                    writePreferences(zip, "customization")
                    val files = File(context.filesDir, "customization")
                    if (files.isDirectory) files.walkTopDown().filter(File::isFile).forEach { file ->
                        zip.putNextEntry(ZipEntry("files/customization/${file.relativeTo(files).invariantSeparatorsPath}"))
                        file.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                    }
                }
            }
        } ?: throw IOException("无法创建备份文件")
    }

    fun restore(uri: Uri) {
        context.contentResolver.openInputStream(uri)?.use { raw ->
            ZipInputStream(raw.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name.startsWith("prefs/") && entry.name.endsWith(".json") -> {
                            val name = entry.name.removePrefix("prefs/").removeSuffix(".json")
                            restorePreferences(name, zip.readBytes())
                        }
                        entry.name.startsWith("files/customization/") && !entry.isDirectory -> {
                            val root = File(context.filesDir, "customization").canonicalFile
                            val target = File(root, entry.name.removePrefix("files/customization/")).canonicalFile
                            if (!target.path.startsWith(root.path + File.separator)) throw IOException("备份包含不安全路径")
                            target.parentFile?.mkdirs(); target.outputStream().use { zip.copyTo(it) }
                        }
                    }
                    zip.closeEntry(); entry = zip.nextEntry
                }
            }
        } ?: throw IOException("无法读取备份文件")
    }

    private fun writePreferences(zip: ZipOutputStream, name: String) {
        val all = context.getSharedPreferences(name, Context.MODE_PRIVATE).all
        val json = JSONObject()
        all.forEach { (key, value) -> json.put(key, value) }
        zip.putNextEntry(ZipEntry("prefs/$name.json")); zip.write(json.toString().toByteArray()); zip.closeEntry()
    }

    private fun restorePreferences(name: String, bytes: ByteArray) {
        if (name !in setOf("neko_weather", "customization", "api_usage", "notification_history")) return
        val json = JSONObject(String(bytes))
        val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear()
        json.keys().forEach { key -> when (val value = json.get(key)) {
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Double -> editor.putFloat(key, value.toFloat())
            else -> editor.putString(key, value.toString())
        } }
        editor.apply()
    }
}
