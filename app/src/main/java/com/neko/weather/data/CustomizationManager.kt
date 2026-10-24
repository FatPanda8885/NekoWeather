package com.neko.weather.data

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import com.neko.weather.model.CustomizationSettings
import com.neko.weather.model.Live2dModelProfile
import com.neko.weather.model.Live2dPlacement
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipInputStream

class CustomizationManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("customization", Context.MODE_PRIVATE)
    private val root = File(context.filesDir, "customization").apply { mkdirs() }

    fun load(): CustomizationSettings {
        val models = loadModels().ifEmpty { migrateLegacyModel() }
        val requestedId = prefs.getString("selectedLive2dId", "").orEmpty()
        val selected = models.firstOrNull { it.id == requestedId } ?: models.firstOrNull()
        if (selected != null && selected.id != requestedId) prefs.edit().putString("selectedLive2dId", selected.id).apply()
        return CustomizationSettings(
            fontPath = prefs.getString("font", "").orEmpty(),
            logoPath = prefs.getString("logo", "").orEmpty(),
            backgroundPath = prefs.getString("background", "").orEmpty(),
            backgroundIsVideo = prefs.getBoolean("backgroundVideo", false),
            weatherBackgrounds = WEATHER_BACKGROUND_KEYS.mapNotNull { key ->
                prefs.getString("weatherBackground_$key", "").orEmpty().takeIf { File(it).isFile }?.let { key to it }
            }.toMap(),
            appIconPath = prefs.getString("appIcon", "").orEmpty().takeIf { File(it).isFile }.orEmpty(),
            live2dModelPath = selected?.modelPath.orEmpty(),
            live2dPreviewPath = selected?.previewPath.orEmpty(),
            live2dModels = models,
            selectedLive2dId = selected?.id.orEmpty(),
            live2dEnabled = prefs.getBoolean("live2dEnabled", true),
            live2dHidden = prefs.getBoolean("live2dHidden", false),
        )
    }

    fun importFile(uri: Uri, kind: String): CustomizationSettings {
        val extension = when (kind) {
            "font" -> ".ttf"
            "logo" -> ".png"
            "appicon" -> ".png"
            "video" -> ".mp4"
            else -> ".jpg"
        }
        val target = File(root, kind + extension)
        context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use(input::copyTo) }
            ?: throw IOException("无法读取所选文件")
        prefs.edit().apply {
            when (kind) {
                "font" -> putString("font", target.absolutePath)
                "logo" -> putString("logo", target.absolutePath)
                "appicon" -> putString("appIcon", target.absolutePath)
                "video" -> putString("background", target.absolutePath).putBoolean("backgroundVideo", true)
                in WEATHER_BACKGROUND_KINDS -> putString("weatherBackground_${kind.removePrefix("weather_")}", target.absolutePath)
                else -> putString("background", target.absolutePath).putBoolean("backgroundVideo", false)
            }
        }.apply()
        return load()
    }

    fun importBitmap(bitmap: Bitmap, kind: String): CustomizationSettings {
        require(kind == "logo" || kind == "appicon" || kind == "background" || kind in WEATHER_BACKGROUND_KINDS) {
            "不支持的图片资源类型"
        }
        val lossless = kind == "logo" || kind == "appicon"
        val target = File(root, kind + if (lossless) ".png" else ".jpg")
        target.outputStream().buffered().use { output ->
            val format = if (lossless) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            if (!bitmap.compress(format, if (lossless) 100 else 92, output)) throw IOException("无法保存编辑后的图片")
        }
        prefs.edit().apply {
            when (kind) {
                "logo" -> putString("logo", target.absolutePath)
                "appicon" -> putString("appIcon", target.absolutePath)
                in WEATHER_BACKGROUND_KINDS -> putString("weatherBackground_${kind.removePrefix("weather_")}", target.absolutePath)
                else -> putString("background", target.absolutePath).putBoolean("backgroundVideo", false)
            }
        }.apply()
        return load()
    }

    fun resetResource(kind: String): CustomizationSettings {
        val editor = prefs.edit()
        when (kind) {
            "font" -> { editor.remove("font"); File(root, "font.ttf").delete() }
            "logo" -> { editor.remove("logo"); File(root, "logo.png").delete() }
            "appicon" -> { editor.remove("appIcon"); File(root, "appicon.png").delete() }
            "background" -> {
                editor.remove("background").putBoolean("backgroundVideo", false)
                File(root, "background.jpg").delete()
                File(root, "video.mp4").delete()
            }
            in WEATHER_BACKGROUND_KINDS -> {
                editor.remove("weatherBackground_${kind.removePrefix("weather_")}")
                File(root, "$kind.jpg").delete()
            }
            else -> throw IllegalArgumentException("不支持恢复该资源")
        }
        editor.apply()
        return load()
    }

    fun importLive2d(uri: Uri): CustomizationSettings {
        val id = UUID.randomUUID().toString()
        val staging = File(root, "live2d-import-$id").apply { deleteRecursively(); mkdirs() }
        val installed = File(root, "live2d-models/$id")
        try {
            extractModel(uri, staging)
            val model = staging.walkTopDown()
                .filter { it.isFile && it.name.endsWith(".model3.json", ignoreCase = true) }
                .minByOrNull { it.relativeTo(staging).invariantSeparatorsPath.count { char -> char == '/' } }
                ?: throw IOException("未找到 .model3.json")
            validateModel(staging, model)
            val previewNames = setOf("preview.png", "preview.jpg", "preview.jpeg", "thumbnail.png", "thumbnail.jpg", "cover.png", "cover.jpg", "portrait.png")
            val preview = model.parentFile?.listFiles()?.firstOrNull { it.isFile && it.name.lowercase() in previewNames }
                ?: staging.walkTopDown().firstOrNull { it.isFile && it.name.lowercase() in previewNames }

            val modelRelative = model.relativeTo(staging).path
            val previewRelative = preview?.relativeTo(staging)?.path
            installed.parentFile?.mkdirs()
            if (!staging.renameTo(installed)) {
                staging.copyRecursively(installed, overwrite = true)
                staging.deleteRecursively()
            }
            val profile = Live2dModelProfile(
                id = id,
                name = model.name.removeSuffix(".model3.json").ifBlank { "Live2D 模型" },
                modelPath = File(installed, modelRelative).absolutePath,
                previewPath = previewRelative?.let { File(installed, it).absolutePath }.orEmpty(),
            )
            saveModels(loadModels() + profile, id)
            prefs.edit().putBoolean("live2dHidden", false).apply()
            return load()
        } catch (error: Exception) {
            staging.deleteRecursively()
            installed.deleteRecursively()
            throw error
        }
    }

    fun selectLive2d(id: String): CustomizationSettings {
        if (loadModels().none { it.id == id }) throw IOException("模型不存在")
        prefs.edit().putString("selectedLive2dId", id).putBoolean("live2dHidden", false).apply()
        return load()
    }

    fun deleteLive2d(id: String): CustomizationSettings {
        val remaining = loadModels().filterNot { profile ->
            if (profile.id == id) File(root, "live2d-models/$id").deleteRecursively()
            profile.id == id
        }
        val selected = prefs.getString("selectedLive2dId", "").orEmpty()
        saveModels(remaining, if (selected == id) remaining.firstOrNull()?.id.orEmpty() else selected)
        return load()
    }

    fun updateLive2dPlacement(id: String, placement: Live2dPlacement): CustomizationSettings {
        val normalized = placement.copy(
            offsetX = placement.offsetX.coerceIn(-500f, 500f),
            offsetY = placement.offsetY.coerceIn(-800f, 800f),
            scale = placement.scale.coerceIn(0.25f, 4f),
            rotation = ((placement.rotation + 180f) % 360f + 360f) % 360f - 180f,
        )
        saveModels(loadModels().map { if (it.id == id) it.copy(placement = normalized) else it }, id)
        return load()
    }

    fun setLive2dHidden(hidden: Boolean): CustomizationSettings {
        prefs.edit().putBoolean("live2dHidden", hidden).apply()
        return load()
    }

    fun setLive2dEnabled(enabled: Boolean): CustomizationSettings {
        prefs.edit().putBoolean("live2dEnabled", enabled).apply()
        return load()
    }

    fun reset(): CustomizationSettings {
        // 保留 presets/ 子目录与外观预设清单：恢复默认只清空当前生效资源。
        root.listFiles()?.filterNot { it.name == "presets" }?.forEach(File::deleteRecursively)
        root.mkdirs()
        val presetsJson = prefs.getString("appearancePresetsJson", null)
        prefs.edit().clear().apply()
        if (presetsJson != null) prefs.edit().putString("appearancePresetsJson", presetsJson).apply()
        return load()
    }

    private fun extractModel(uri: Uri, staging: File) {
        var entryCount = 0
        var totalBytes = 0L
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (entry != null) {
                    if (++entryCount > MAX_LIVE2D_ENTRIES) throw IOException("模型包文件数量过多")
                    val output = File(staging, entry.name).canonicalFile
                    if (!output.path.startsWith(staging.canonicalPath + File.separator)) throw IOException("模型包包含不安全路径")
                    if (entry.isDirectory) output.mkdirs() else {
                        output.parentFile?.mkdirs()
                        output.outputStream().buffered().use { destination ->
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                totalBytes += count
                                if (totalBytes > MAX_LIVE2D_BYTES) throw IOException("模型包解压后超过 300 MB")
                                destination.write(buffer, 0, count)
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: throw IOException("无法读取模型包")
    }

    private fun validateModel(staging: File, model: File) {
        val references = JSONObject(model.readText()).optJSONObject("FileReferences")
            ?: throw IOException("model3.json 缺少 FileReferences")
        fun referencedFile(path: String): File {
            val file = File(model.parentFile, path.replace('\\', '/')).canonicalFile
            if (!file.path.startsWith(staging.canonicalPath + File.separator) || !file.isFile) {
                throw IOException("模型引用的资源不存在：$path")
            }
            return file
        }
        referencedFile(references.optString("Moc").ifBlank { throw IOException("model3.json 未声明 Moc") })
        val textures = references.optJSONArray("Textures") ?: throw IOException("model3.json 未声明纹理")
        if (textures.length() == 0) throw IOException("模型没有纹理")
        for (index in 0 until textures.length()) referencedFile(textures.getString(index))
    }

    private fun loadModels(): List<Live2dModelProfile> = runCatching {
        val array = JSONArray(prefs.getString("live2dModelsJson", "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val modelPath = item.optString("modelPath")
                if (!File(modelPath).isFile) continue
                add(
                    Live2dModelProfile(
                        id = item.getString("id"),
                        name = item.optString("name", "Live2D 模型"),
                        modelPath = modelPath,
                        previewPath = item.optString("previewPath").takeIf { File(it).isFile }.orEmpty(),
                        placement = item.optJSONObject("placement")?.let {
                            Live2dPlacement(
                                offsetX = it.optDouble("offsetX", 0.0).toFloat(),
                                offsetY = it.optDouble("offsetY", 0.0).toFloat(),
                                scale = it.optDouble("scale", 1.0).toFloat(),
                                rotation = it.optDouble("rotation", 0.0).toFloat(),
                            )
                        } ?: Live2dPlacement(),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun saveModels(models: List<Live2dModelProfile>, selectedId: String) {
        val array = JSONArray()
        models.forEach { model ->
            array.put(
                JSONObject()
                    .put("id", model.id)
                    .put("name", model.name)
                    .put("modelPath", model.modelPath)
                    .put("previewPath", model.previewPath)
                    .put(
                        "placement",
                        JSONObject()
                            .put("offsetX", model.placement.offsetX)
                            .put("offsetY", model.placement.offsetY)
                            .put("scale", model.placement.scale)
                            .put("rotation", model.placement.rotation),
                    ),
            )
        }
        prefs.edit().putString("live2dModelsJson", array.toString()).putString("selectedLive2dId", selectedId).apply()
    }

    private fun migrateLegacyModel(): List<Live2dModelProfile> {
        val path = prefs.getString("live2d", "").orEmpty()
        if (!File(path).isFile) return emptyList()
        val preview = prefs.getString("live2dPreview", "").orEmpty().takeIf { File(it).isFile }.orEmpty()
        val profile = Live2dModelProfile("legacy", File(path).name.removeSuffix(".model3.json"), path, preview)
        saveModels(listOf(profile), profile.id)
        return listOf(profile)
    }

    private companion object {
        const val MAX_LIVE2D_ENTRIES = 5_000
        const val MAX_LIVE2D_BYTES = 300L * 1024L * 1024L
        val WEATHER_BACKGROUND_KEYS = listOf("clear", "cloudy", "rain", "snow", "fog", "thunder")
        val WEATHER_BACKGROUND_KINDS = WEATHER_BACKGROUND_KEYS.map { "weather_$it" }.toSet()
    }
}
