package com.neko.weather.data

import com.neko.weather.model.UpdateInfo
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class UpdateRepository(private val client: OkHttpClient = OkHttpClient()) {
    fun check(currentVersion: String): UpdateInfo {
        val source = updateSourceFor(detectMainlandIp())
        val request = Request.Builder().url(source.latestReleaseApi).header("Accept", source.accept)
            .header("User-Agent", "NekoWeather/$currentVersion").build()
        val json = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val detail = if (source.name == "Gitee" && response.code == 404) {
                    "Gitee 仓库尚未发布 Release"
                } else {
                    "更新服务器返回 HTTP ${response.code}"
                }
                throw IOException(detail)
            }
            JSONObject(response.body?.string().orEmpty())
        }
        val latest = json.optString("tag_name").trim().removePrefix("v")
        if (latest.isBlank()) throw IOException("Release 未提供版本号")
        val assets = json.optJSONArray("assets")
        val apk = (0 until (assets?.length() ?: 0)).mapNotNull { assets?.optJSONObject(it) }
            .firstOrNull { it.optString("name").endsWith(".apk", true) }
            ?.optString("browser_download_url")?.takeIf(String::isNotBlank)
        return UpdateInfo(
            version = latest,
            changelog = json.optString("body").ifBlank { "本次 Release 未填写更新日志。" },
            pageUrl = json.optString("html_url").ifBlank { source.releasePage },
            downloadUrl = apk,
            source = source.name,
            updateAvailable = compareVersions(latest, currentVersion) > 0,
        )
    }

    internal fun updateSourceFor(mainland: Boolean): UpdateSource = if (mainland) {
        UpdateSource(
            name = "Gitee",
            latestReleaseApi = GITEE_LATEST,
            releasePage = GITEE_RELEASES,
            accept = "application/json",
        )
    } else {
        UpdateSource(
            name = "GitHub",
            latestReleaseApi = GITHUB_LATEST,
            releasePage = GITHUB_RELEASES,
            accept = "application/vnd.github+json",
        )
    }

    private fun detectMainlandIp(): Boolean = runCatching {
        val request = Request.Builder().url("https://www.cloudflare.com/cdn-cgi/trace".toHttpUrl())
            .header("User-Agent", "NekoWeather/0.1").build()
        client.newCall(request).execute().use { response ->
            response.isSuccessful && response.body?.string().orEmpty().lineSequence().any { it.trim() == "loc=CN" }
        }
    }.getOrDefault(false)

    internal fun compareVersions(left: String, right: String): Int {
        val a = left.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val b = right.removePrefix("v").substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(a.size, b.size)) {
            val comparison = (a.getOrElse(index) { 0 }).compareTo(b.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    private companion object {
        const val GITHUB_LATEST = "https://api.github.com/repos/FatPanda8885/NekoWeather/releases/latest"
        const val GITHUB_RELEASES = "https://github.com/FatPanda8885/NekoWeather/releases/latest"
        const val GITEE_LATEST = "https://gitee.com/api/v5/repos/pasky/NekoWeather/releases/latest"
        const val GITEE_RELEASES = "https://gitee.com/pasky/NekoWeather/releases"
    }
}

internal data class UpdateSource(
    val name: String,
    val latestReleaseApi: String,
    val releasePage: String,
    val accept: String,
)
