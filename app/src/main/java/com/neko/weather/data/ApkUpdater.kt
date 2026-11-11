package com.neko.weather.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** 下载/校验阶段的自有失败：message 已经是可直接展示的中文文案。 */
class UpdateException(message: String, cause: Throwable? = null) : IOException(message, cause)

/** 应用内下载 release APK、校验完整性并唤起系统安装器。 */
object ApkUpdater {
    private const val MIME_APK = "application/vnd.android.package-archive"
    private const val GENERIC_DOWNLOAD_ERROR = "下载更新失败，请重试"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    fun apkFileName(version: String): String =
        "NekoWeather-${version.replace(Regex("[^0-9A-Za-z._-]"), "")}.apk"

    fun download(context: Context, url: String, fileName: String, onProgress: (Float?) -> Unit): File =
        downloadInto(File(context.cacheDir, "updates"), url, fileName, onProgress)

    /** 已经下载并通过校验的 APK；用于「授权安装回来」时直接复用，避免整包重下。 */
    fun findVerified(context: Context, fileName: String): File? {
        val target = File(File(context.cacheDir, "updates"), fileName)
        if (!target.isFile) return null
        return runCatching { verify(context, target) }.map { target }.getOrNull()
    }

    /**
     * 下载 APK 到 [dir]，支持断点续传。
     *
     * 元数据（url / ETag / 总长度）在**收到响应头之后、读取响应体之前**就落盘：
     * 否则中途断流或进程被杀时只会留下 .part 而没有 ETag，重试时无法构造 If-Range，
     * 续传就退化成了重新下载。
     *
     * 服务端返回 200（不支持 Range 或 ETag 已变）时清空半成品重新开始；没有 ETag 时不续传，
     * 避免把两份不同内容拼成坏文件。
     */
    internal fun downloadInto(dir: File, url: String, fileName: String, onProgress: (Float?) -> Unit): File {
        dir.mkdirs()
        val target = File(dir, fileName)
        val part = File(dir, "$fileName.part")
        val meta = File(dir, "$fileName.meta")

        // 换版本或换源时，清掉不属于本次下载的半成品
        dir.listFiles()
            ?.filterNot { it.name == fileName || it.name.startsWith("$fileName.") }
            ?.forEach { it.delete() }

        val saved = runCatching { JSONObject(meta.readText()) }.getOrNull()
        if (saved == null || saved.optString("url") != url) {
            part.delete()
            meta.delete()
        }
        val resumeFrom = part.length()
        val savedEtag = runCatching { JSONObject(meta.readText()) }.getOrNull()?.optString("etag").orEmpty()
        val canResume = resumeFrom > 0L && savedEtag.isNotBlank()

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/octet-stream")
            .apply {
                if (canResume) {
                    header("Range", "bytes=$resumeFrom-")
                    header("If-Range", savedEtag)
                }
            }
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val appending: Boolean
                val total: Long?
                when (response.code) {
                    200 -> {
                        appending = false
                        total = response.body?.contentLength()?.takeIf { it > 0L }
                        part.delete()
                    }
                    206 -> {
                        appending = true
                        total = parseContentRangeTotal(response.header("Content-Range"))
                            ?: response.body?.contentLength()?.takeIf { it > 0L }?.let { resumeFrom + it }
                    }
                    416 -> {
                        part.delete()
                        meta.delete()
                        throw UpdateException("更新文件已失效，请重试")
                    }
                    else -> throw UpdateException("下载更新失败：HTTP ${response.code}")
                }
                // 关键：元数据先落盘，再开始读响应体。中断后重试才有 ETag 可用。
                writeMeta(meta, url, response.header("ETag").orEmpty(), total)

                val body = response.body ?: throw UpdateException("下载更新失败：响应为空")
                var written = if (appending) resumeFrom else 0L
                body.byteStream().use { input ->
                    FileOutputStream(part, appending).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            written += read
                            onProgress(progressFraction(written, total))
                        }
                    }
                }
            }
        } catch (error: UpdateException) {
            throw error
        } catch (error: IOException) {
            // 底层异常（连接重置、超时、断流）原文是英文技术串，不能直接展示给用户
            throw UpdateException(GENERIC_DOWNLOAD_ERROR, error)
        }

        val expected = runCatching { JSONObject(meta.readText()).optLong("total", -1L) }.getOrDefault(-1L)
        if (part.length() <= 0L) throw UpdateException("下载更新失败：文件为空")
        if (expected > 0L && part.length() != expected) throw UpdateException("更新文件不完整，请重试")
        if (target.exists()) target.delete()
        if (!part.renameTo(target)) throw UpdateException("更新文件重命名失败")
        meta.delete()
        onProgress(1f)
        return target
    }

    private fun writeMeta(meta: File, url: String, etag: String, total: Long?) {
        runCatching {
            meta.writeText(
                JSONObject()
                    .put("url", url)
                    .put("etag", etag)
                    .put("total", total ?: -1L)
                    .toString(),
            )
        }
    }

    /**
     * 校验已下载的 APK：能解析、包名一致、签名证书与已安装应用一致。
     * 最后一条才是真正的完整性保证——替换过的包不可能带着同样的签名。
     */
    fun verify(context: Context, apk: File) {
        val packageManager = context.packageManager
        val archive = packageManager.getPackageArchiveInfo(apk.absolutePath, signingFlags())
            ?: throw UpdateException("更新文件不是有效的安装包")
        if (archive.packageName != context.packageName) throw UpdateException("更新文件包名不匹配")
        val apkSigners = archive.signerDigests()
        val installedSigners = packageManager.getPackageInfo(context.packageName, signingFlags()).signerDigests()
        if (apkSigners.isEmpty() || apkSigners != installedSigners) {
            throw UpdateException("更新文件签名与当前应用不一致")
        }
    }

    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun requestInstallPermission(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    /** 唤起系统安装器；未授权时改为打开授权页并返回 false。 */
    fun install(context: Context, apk: File): Boolean {
        if (!canInstall(context)) {
            requestInstallPermission(context)
            return false
        }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, MIME_APK)
            clipData = ClipData.newRawUri(apk.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // 部分 ROM 用第三方安装器接管。URI 授权可能落在被解析到的首个组件上，而真正读文件的
        // 是另一个包，因此把所有候选安装器都显式授权一次。
        runCatching {
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                .forEach { info ->
                    runCatching {
                        context.grantUriPermission(
                            info.activityInfo.packageName,
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                }
        }
        context.startActivity(intent)
        return true
    }

    private fun signingFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

    @Suppress("DEPRECATION")
    private fun PackageInfo.signerDigests(): Set<String> {
        val certs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signingInfo?.apkContentsSigners
        } else {
            signatures
        } ?: return emptySet()
        return certs.mapTo(LinkedHashSet()) { signature ->
            MessageDigest.getInstance("SHA-256")
                .digest(signature.toByteArray())
                .joinToString("") { byte -> "%02x".format(byte) }
        }
    }
}

/** 从 Content-Range（bytes 1000-9999/10000）取出总长度；通配符或异常格式返回 null。 */
internal fun parseContentRangeTotal(header: String?): Long? {
    val value = header?.substringAfterLast('/', "")?.trim().orEmpty()
    if (value.isEmpty() || value == "*") return null
    return value.toLongOrNull()?.takeIf { it > 0L }
}

/** 下载进度；总长度未知（或非法）时返回 null，界面据此显示不确定进度。 */
internal fun progressFraction(downloaded: Long, total: Long?): Float? =
    total?.takeIf { it > 0L }?.let { (downloaded.toFloat() / it).coerceIn(0f, 1f) }
