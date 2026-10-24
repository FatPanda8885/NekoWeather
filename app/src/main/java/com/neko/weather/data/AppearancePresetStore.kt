package com.neko.weather.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.neko.weather.model.AppearancePreset
import com.neko.weather.model.AppearancePresets
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 外观预设存储：预设是「皮肤四类资源路径」的组合，文件保存在
 * filesDir/customization/presets/<id>/ 下；应用预设即把预设内路径写入
 * customization SharedPreferences 的现有键，主页渲染逻辑保持不变。
 */
class AppearancePresetStore(context: Context) {
    private val prefs = context.getSharedPreferences("customization", Context.MODE_PRIVATE)
    private val root = File(context.filesDir, "customization/presets").apply { mkdirs() }
    private val contentResolver = context.contentResolver

    companion object {
        private const val KEY = "appearancePresetsJson"
        private const val MIGRATED_ID = "legacy-appearance"
        private const val ZIP_FORMAT = "NekoWeatherAppearancePreset"
        private const val MAX_PRESET_ENTRIES = 40
        private const val MAX_PRESET_BYTES = 300L * 1024L * 1024L
        private val WEATHER_KEYS = listOf("clear", "cloudy", "rain", "snow", "fog", "thunder")
        val RESOURCE_KINDS = setOf("font", "logo", "background", "video") + WEATHER_KEYS.map { "weather_$it" }
    }

    /** 读取预设；首次读取时把已有的单份皮肤资源迁移成「我的外观」预设。 */
    fun load(): AppearancePresets {
        val raw = prefs.getString(KEY, null) ?: return migrateLegacy().also(::save)
        return runCatching { parse(JSONObject(raw)) }.getOrElse { migrateLegacy().also(::save) }
    }

    fun save(value: AppearancePresets) {
        val root = JSONObject()
            .put("selectedId", value.selectedId)
            .put("presets", JSONArray(value.presets.map(::presetJson)))
        prefs.edit().putString(KEY, root.toString()).apply()
    }

    /** 应用预设：把四类资源路径写入当前外观配置；路径失效的资源按未设置处理。 */
    fun apply(preset: AppearancePreset): AppearancePresets {
        val fontPath = preset.fontPath.takeIf { File(it).isFile }.orEmpty()
        val logoPath = preset.logoPath.takeIf { File(it).isFile }.orEmpty()
        val backgroundPath = preset.backgroundPath.takeIf { File(it).isFile }.orEmpty()
        val backgrounds = WEATHER_KEYS.mapNotNull { key ->
            preset.weatherBackgrounds[key]?.takeIf { File(it).isFile }?.let { key to it }
        }.toMap()
        prefs.edit().apply {
            if (fontPath.isBlank()) remove("font") else putString("font", fontPath)
            if (logoPath.isBlank()) remove("logo") else putString("logo", logoPath)
            if (backgroundPath.isBlank()) {
                remove("background").putBoolean("backgroundVideo", false)
            } else {
                putString("background", backgroundPath).putBoolean("backgroundVideo", preset.backgroundIsVideo)
            }
            WEATHER_KEYS.forEach { key ->
                val path = backgrounds[key]
                if (path.isNullOrBlank()) remove("weatherBackground_$key") else putString("weatherBackground_$key", path)
            }
        }.apply()
        val current = load()
        val updated = current.copy(selectedId = preset.id)
        save(updated)
        return updated
    }

    /** 以当前生效的外观为内容新建预设（预设只是路径组合，不复制文件）。 */
    fun createFromCurrent(name: String): AppearancePresets {
        val current = load()
        val preset = AppearancePreset(
            id = UUID.randomUUID().toString(),
            name = name,
            fontPath = prefs.getString("font", "").orEmpty(),
            logoPath = prefs.getString("logo", "").orEmpty(),
            backgroundPath = prefs.getString("background", "").orEmpty(),
            backgroundIsVideo = prefs.getBoolean("backgroundVideo", false),
            weatherBackgrounds = WEATHER_KEYS.mapNotNull { key ->
                prefs.getString("weatherBackground_$key", null)?.let { key to it }
            }.toMap(),
        )
        val updated = current.copy(presets = current.presets + preset, selectedId = preset.id)
        save(updated)
        return updated
    }

    fun rename(id: String, name: String): AppearancePresets {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return load()
        val current = load()
        val updated = current.copy(presets = current.presets.map { if (it.id == id) it.copy(name = trimmed) else it })
        save(updated)
        return updated
    }

    /** 删除预设；若删除的是当前应用中的预设，则四类资源一并恢复默认。 */
    fun delete(ids: Set<String>): AppearancePresets {
        if (ids.isEmpty()) return load()
        val current = load()
        val remaining = current.presets.filterNot { it.id in ids }
        ids.forEach { id -> File(root, id).deleteRecursively() }
        val selectedLeft = current.selectedId !in ids && remaining.any { it.id == current.selectedId }
        val updated = current.copy(presets = remaining, selectedId = if (selectedLeft) current.selectedId else "")
        save(updated)
        if (!selectedLeft) apply(AppearancePreset(id = "", name = ""))
        return updated
    }

    /** 恢复默认外观后清空选中状态，但保留预设列表本身。 */
    fun clearSelection(): AppearancePresets {
        val updated = load().copy(selectedId = "")
        save(updated)
        return updated
    }

    /** 把用户选择的文件导入预设目录；若该预设正在使用则同步当前外观。 */
    fun importResource(presetId: String, uri: Uri, kind: String): AppearancePresets {
        val preset = find(presetId) ?: throw IOException("预设不存在")
        val target = File(presetDir(presetId), resourceFileName(kind))
        contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use(input::copyTo)
        } ?: throw IOException("无法读取所选文件")
        return updateResource(preset, kind) { target.absolutePath }
    }

    /** 把裁剪后的位图写入预设目录；若该预设正在使用则同步当前外观。 */
    fun importBitmap(presetId: String, bitmap: Bitmap, kind: String): AppearancePresets {
        val preset = find(presetId) ?: throw IOException("预设不存在")
        val lossless = kind == "logo"
        val target = File(presetDir(presetId), resourceFileName(kind))
        target.outputStream().buffered().use { output ->
            val format = if (lossless) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            if (!bitmap.compress(format, if (lossless) 100 else 92, output)) throw IOException("无法保存编辑后的图片")
        }
        return updateResource(preset, kind) { target.absolutePath }
    }

    /** 移除预设中的某类资源；若该预设正在使用则同步恢复默认。 */
    fun resetResource(presetId: String, kind: String): AppearancePresets {
        val preset = find(presetId) ?: throw IOException("预设不存在")
        File(presetDir(presetId), resourceFileName(kind)).delete()
        return updateResource(preset, kind) { "" }
    }

    /** 从 ZIP 导入新预设：manifest.json + 平铺的资源文件。 */
    fun importPreset(uri: Uri): AppearancePresets {
        val staging = File(root, "import-${UUID.randomUUID()}").apply { deleteRecursively(); mkdirs() }
        try {
            extractPresetZip(uri, staging)
            val manifestFile = File(staging, "manifest.json")
            if (!manifestFile.isFile) throw IOException("预设包缺少 manifest.json")
            val manifest = runCatching { JSONObject(manifestFile.readText()) }.getOrNull()
                ?: throw IOException("manifest.json 不是有效的 JSON")
            if (manifest.optString("format") != ZIP_FORMAT) throw IOException("这不是 NekoWeather 外观预设包")
            val name = manifest.optString("name").ifBlank { "导入的预设" }
            val resources = manifest.optJSONObject("resources") ?: JSONObject()
            fun resolved(kind: String): String {
                val fileName = resources.optString(kind)
                if (fileName.isBlank()) return ""
                val file = File(staging, fileName).canonicalFile
                if (!file.path.startsWith(staging.canonicalPath + File.separator) || !file.isFile) return ""
                return file.absolutePath
            }
            val weatherBackgrounds = WEATHER_KEYS.mapNotNull { key ->
                resolved("weather_$key").takeIf(String::isNotBlank)?.let { key to it }
            }.toMap()
            val id = UUID.randomUUID().toString()
            val installed = File(root, id)
            if (!staging.renameTo(installed)) {
                staging.copyRecursively(installed, overwrite = true)
                staging.deleteRecursively()
            }
            val preset = AppearancePreset(
                id = id,
                name = name,
                fontPath = resolved("font"),
                logoPath = resolved("logo"),
                backgroundPath = resolved("background"),
                backgroundIsVideo = resources.optBoolean("backgroundIsVideo", false) && resolved("background").isNotBlank(),
                weatherBackgrounds = weatherBackgrounds,
            )
            val current = load()
            val updated = current.copy(presets = current.presets + preset)
            save(updated)
            return updated
        } catch (error: Exception) {
            staging.deleteRecursively()
            throw error
        }
    }

    /** 把预设导出为 ZIP：manifest.json + 资源文件平铺打包。 */
    fun exportPreset(preset: AppearancePreset, uri: Uri) {
        contentResolver.openOutputStream(uri, "w")?.use { raw ->
            ZipOutputStream(raw.buffered()).use { zip ->
                val resources = JSONObject()
                fun putResource(kind: String, path: String): String? {
                    if (path.isBlank()) return null
                    val file = File(path)
                    if (!file.isFile) return null
                    zip.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                    resources.put(kind, file.name)
                    return file.name
                }
                putResource("font", preset.fontPath)
                putResource("logo", preset.logoPath)
                putResource("background", preset.backgroundPath)
                if (preset.backgroundIsVideo) resources.put("backgroundIsVideo", true)
                preset.weatherBackgrounds.forEach { (key, path) -> putResource("weather_$key", path) }
                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(
                    JSONObject()
                        .put("format", ZIP_FORMAT)
                        .put("version", 1)
                        .put("name", preset.name)
                        .put("resources", resources)
                        .toString(2)
                        .toByteArray(),
                )
                zip.closeEntry()
            }
        } ?: throw IOException("无法创建导出文件")
    }

    private fun presetDir(id: String) = File(root, id)

    private fun find(id: String): AppearancePreset? = load().presets.firstOrNull { it.id == id }

    private fun updateResource(preset: AppearancePreset, kind: String, newPath: () -> String): AppearancePresets {
        val path = newPath()
        val updatedPreset = when {
            kind == "font" -> preset.copy(fontPath = path)
            kind == "logo" -> preset.copy(logoPath = path)
            kind == "background" -> preset.copy(backgroundPath = path, backgroundIsVideo = false)
            kind == "video" -> preset.copy(backgroundPath = path, backgroundIsVideo = true)
            kind.startsWith("weather_") -> {
                val key = kind.removePrefix("weather_")
                val backgrounds = if (path.isBlank()) {
                    preset.weatherBackgrounds - key
                } else {
                    preset.weatherBackgrounds + (key to path)
                }
                preset.copy(weatherBackgrounds = backgrounds)
            }
            else -> throw IllegalArgumentException("不支持的资源类型")
        }
        val current = load()
        val updated = current.copy(
            presets = current.presets.map { if (it.id == updatedPreset.id) updatedPreset else it },
        )
        save(updated)
        if (updated.selectedId == updatedPreset.id) apply(updatedPreset)
        return updated
    }

    private fun resourceFileName(kind: String): String = when (kind) {
        "font" -> "font.ttf"
        "logo" -> "logo.png"
        "video" -> "video.mp4"
        else -> "$kind.jpg"
    }

    private fun extractPresetZip(uri: Uri, staging: File) {
        var entryCount = 0
        var totalBytes = 0L
        contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (entry != null) {
                    if (++entryCount > MAX_PRESET_ENTRIES) throw IOException("预设包文件数量过多")
                    val output = File(staging, entry.name).canonicalFile
                    if (!output.path.startsWith(staging.canonicalPath + File.separator)) throw IOException("预设包包含不安全路径")
                    if (entry.isDirectory) output.mkdirs() else {
                        output.parentFile?.mkdirs()
                        output.outputStream().buffered().use { destination ->
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                totalBytes += count
                                if (totalBytes > MAX_PRESET_BYTES) throw IOException("预设包解压后超过 300 MB")
                                destination.write(buffer, 0, count)
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: throw IOException("无法读取预设包")
    }

    private fun migrateLegacy(): AppearancePresets {
        val fontPath = prefs.getString("font", "").orEmpty().takeIf { File(it).isFile }.orEmpty()
        val logoPath = prefs.getString("logo", "").orEmpty().takeIf { File(it).isFile }.orEmpty()
        val backgroundPath = prefs.getString("background", "").orEmpty().takeIf { File(it).isFile }.orEmpty()
        val weatherBackgrounds = WEATHER_KEYS.mapNotNull { key ->
            prefs.getString("weatherBackground_$key", null)?.takeIf { File(it).isFile }?.let { key to it }
        }.toMap()
        if (fontPath.isBlank() && logoPath.isBlank() && backgroundPath.isBlank() && weatherBackgrounds.isEmpty()) {
            return AppearancePresets()
        }
        val preset = AppearancePreset(
            id = MIGRATED_ID,
            name = "我的外观",
            fontPath = fontPath,
            logoPath = logoPath,
            backgroundPath = backgroundPath,
            backgroundIsVideo = prefs.getBoolean("backgroundVideo", false),
            weatherBackgrounds = weatherBackgrounds,
        )
        return AppearancePresets(presets = listOf(preset), selectedId = preset.id)
    }

    private fun parse(root: JSONObject): AppearancePresets {
        val presets = root.optJSONArray("presets").objects().mapNotNull { item ->
            val id = item.optString("id").takeIf(String::isNotBlank) ?: return@mapNotNull null
            AppearancePreset(
                id = id,
                name = item.optString("name").ifBlank { "未命名预设" },
                fontPath = item.optString("fontPath"),
                logoPath = item.optString("logoPath"),
                backgroundPath = item.optString("backgroundPath"),
                backgroundIsVideo = item.optBoolean("backgroundIsVideo"),
                weatherBackgrounds = item.optJSONObject("weatherBackgrounds")?.keys()?.asSequence()
                    ?.mapNotNull { key -> item.optJSONObject("weatherBackgrounds").optString(key).takeIf(String::isNotBlank)?.let { key to it } }
                    ?.toMap() ?: emptyMap(),
            )
        }.distinctBy { it.id }
        val selectedId = root.optString("selectedId").takeIf { id -> presets.any { it.id == id } }
            ?: presets.firstOrNull()?.id.orEmpty()
        return AppearancePresets(presets, selectedId)
    }

    private fun presetJson(it: AppearancePreset) = JSONObject()
        .put("id", it.id)
        .put("name", it.name)
        .put("fontPath", it.fontPath)
        .put("logoPath", it.logoPath)
        .put("backgroundPath", it.backgroundPath)
        .put("backgroundIsVideo", it.backgroundIsVideo)
        .put("weatherBackgrounds", JSONObject(it.weatherBackgrounds))

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else
        (0 until length()).mapNotNull(::optJSONObject)
}
