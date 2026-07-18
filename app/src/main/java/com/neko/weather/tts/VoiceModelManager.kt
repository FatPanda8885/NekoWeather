package com.neko.weather.tts

import android.content.Context
import android.net.Uri
import com.neko.weather.model.ImportedVoiceModel
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.zip.ZipInputStream

class VoiceModelManager(private val context: Context) {
    private val modelRoot = File(context.filesDir, "voice_models").apply { mkdirs() }

    fun list(): List<ImportedVoiceModel> = modelRoot.listFiles()
        ?.filter(File::isDirectory)
        ?.mapNotNull(::readModel)
        ?.sortedBy { it.name }
        .orEmpty()

    fun import(uri: Uri): ImportedVoiceModel {
        val id = UUID.randomUUID().toString()
        val target = File(modelRoot, id).apply { mkdirs() }
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val output = File(target, entry.name).canonicalFile
                        if (!output.path.startsWith(target.canonicalPath + File.separator)) {
                            throw IOException("模型包包含不安全路径")
                        }
                        if (entry.isDirectory) output.mkdirs() else {
                            output.parentFile?.mkdirs()
                            output.outputStream().buffered().use { zip.copyTo(it) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: throw IOException("无法读取所选文件")
            return readModel(target) ?: throw IOException(
                "模型包无效：需要 manifest.json、model.onnx 和 tokens.txt",
            )
        } catch (error: Exception) {
            target.deleteRecursively()
            throw error
        }
    }

    fun delete(id: String) {
        modelRoot.resolve(id).takeIf { it.parentFile == modelRoot }?.deleteRecursively()
    }

    private fun readModel(directory: File): ImportedVoiceModel? = runCatching {
        val manifestFile = directory.resolve("manifest.json")
        val modelFile = directory.resolve("model.onnx")
        val tokensFile = directory.resolve("tokens.txt")
        if (!manifestFile.isFile || !modelFile.isFile || !tokensFile.isFile) return null
        val json = JSONObject(manifestFile.readText())
        ImportedVoiceModel(
            id = directory.name,
            name = json.getString("name"),
            engine = json.optString("engine", "vits"),
            language = json.optString("language", "zh-CN"),
            sampleRate = json.optInt("sampleRate", 22050),
            directoryPath = directory.absolutePath,
        )
    }.getOrNull()
}

