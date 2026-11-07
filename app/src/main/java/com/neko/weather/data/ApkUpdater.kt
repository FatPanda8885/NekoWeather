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

/** 应用内下载 release APK、校验完整性并唤起系统安装器。 */
object ApkUpdater {
    private const val MIME_APK = "application/vnd.android.package-archive"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * 下载 APK 到 cache/updates/（该目录已由 file_paths.xml 暴露给 FileProvider）。
     *
     * 断点续传：半成品 `.part` 与 ETag 记录在同目录，下次同一个下载地址时用
     * `Range` + `If-Range` 续传；服务端若返回 200（不支持 Range）或文件已变（ETag 不匹配）
     * 则自动从头开始。没有 ETag 时不做续传，避免把两段不同内容拼在一起。
     *
     * [onProgress] 收到 0..1；服务端未给总长度时收到 null，由界面显示为不确定进度。
     */
    fun download(context: Context, url: String, fileName: String, onProgress: (Float?) -> Unit): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(dir, fileName)
        val part = File(dir, "$fileName.part")
        val meta = File(dir, "$fileName.meta")

        // 换版本或换源时，把上一份半成品清掉
        dir.listFiles()
            ?.filterNot { it.name == fileName || it.name.startsWith("$fileName.") }
            ?.forEach { it.delete() }

        val saved = runCatching { JSONObject(meta.readText()) }.getOrNull()
        if (saved == null || saved.optString("url") != url) {
            part.delete()
            meta.delete()
        }
        val resumeFrom = part.length()
        val savedEtag = runCatching { JSONObject(meta.readText()) }.getOrNull()
            ?.optString("etag").orEmpty()
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

        client.newCall(request).execute().use { response ->
            val appending: Boolean
            val total: Long?
            when (response.code) {
                200 -> {
                    // 服务端忽略了 Range，或本来就是全新下载
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
                    // 本地半成品与服务端已对不上
                    part.delete()
                    meta.delete()
                    throw IOException("更新文件已失效，请重试")
                }
                else -> throw IOException("下载更新失败：HTTP ${response.code}")
            }
            val body = response.body ?: throw IOException("下载更新失败：响应为空")
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
            runCatching {
                meta.writeText(
                    JSONObject()
                        .put("url", url)
                        .put("etag", response.header("ETag").orEmpty())
                        .put("total", total ?: -1L)
                        .toString(),
                )
            }
        }

        val expected = runCatching { JSONObject(meta.readText()).optLong("total", -1L) }.getOrDefault(-1L)
        if (part.length() <= 0L) throw IOException("下载更新失败：文件为空")
        if (expected > 0L && part.length() != expected) {
            throw IOException("更新文件不完整，请重试")
        }
        if (target.exists()) target.delete()
        if (!part.renameTo(target)) throw IOException("更新文件重命名失败")
        meta.delete()
        onProgress(1f)
        return target
    }

    /**
     * 校验已下载的 APK：必须是能解析的安装包、包名与本应用一致、签名证书与当前已安装应用一致。
     * 最后一条是真正的完整性保证——被替换过的安装包不可能带着同样的签名，
     * 而签名不一致的包即便交给系统安装器也会被拒绝。
     */
    fun verify(context: Context, apk: File) {
        val packageManager = context.packageManager
        val archive = packageManager.getPackageArchiveInfo(apk.absolutePath, signingFlags())
            ?: throw IOException("更新文件不是有效的安装包")
        if (archive.packageName != context.packageName) throw IOException("更新文件包名不匹配")
        val apkSigners = archive.signerDigests()
        val installedSigners = packageManager.getPackageInfo(context.packageName, signingFlags()).signerDigests()
        if (apkSigners.isEmpty() || apkSigners != installedSigners) {
            throw IOException("更新文件签名与当前应用不一致")
        }
    }

    /** 是否已允许本应用安装未知来源应用。 */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    /** 打开「安装未知应用」的授权页；用户授权后需重试安装。 */
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

/** 从 `Content-Range: bytes 1000-9999/10000` 取出总长度；通配符或异常格式返回 null。 */
internal fun parseContentRangeTotal(header: String?): Long? {
    val value = header?.substringAfterLast('/', "")?.trim().orEmpty()
    if (value.isEmpty() || value == "*") return null
    return value.toLongOrNull()?.takeIf { it > 0L }
}

/** 下载进度；总长度未知（或非法）时返回 null，界面据此显示不确定进度。 */
internal fun progressFraction(downloaded: Long, total: Long?): Float? =
    total?.takeIf { it > 0L }?.let { (downloaded.toFloat() / it).coerceIn(0f, 1f) }
