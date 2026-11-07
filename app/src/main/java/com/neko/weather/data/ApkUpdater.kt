package com.neko.weather.data

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** 应用内下载 release APK 并唤起系统安装器。 */
object ApkUpdater {
    private const val MIME_APK = "application/vnd.android.package-archive"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * 下载 APK 到 cache/updates/（该目录已由 file_paths.xml 暴露给 FileProvider）。
     * [onProgress] 收到 0..1；服务端未给 Content-Length 时收到 null，由界面显示为不确定进度。
     */
    fun download(context: Context, url: String, fileName: String, onProgress: (Float?) -> Unit): File {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val target = File(dir, fileName)
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/octet-stream")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("下载更新失败：HTTP ${response.code}")
            val body = response.body ?: throw IOException("下载更新失败：响应为空")
            val total = body.contentLength().takeIf { it > 0L }
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var written = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read
                        onProgress(total?.let { (written.toFloat() / it).coerceIn(0f, 1f) })
                    }
                }
            }
        }
        if (target.length() <= 0L) throw IOException("下载更新失败：文件为空")
        return target
    }

    /** 是否已允许本应用安装未知来源应用。 */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    /** 打开「安装未知应用」的授权页；用户授权后需手动重试安装。 */
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
}
