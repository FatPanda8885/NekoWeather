package com.neko.weather.data

import android.os.Build
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
        val candidates = (0 until (assets?.length() ?: 0)).mapNotNull { assets?.optJSONObject(it) }
            .map { it.optString("name") to it.optString("browser_download_url") }
        val apk = selectApkUrl(candidates, Build.SUPPORTED_ABIS.toList())
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

    /**
     * 从 Release 的 assets 里挑出与本机架构匹配的 APK。
     *
     * release 按 ABI 拆包，同时会产出全架构通用包，所以不能只取第一个 .apk：
     * 装上错误架构的包会直接安装失败。优先按设备 [supportedAbis] 顺序精确匹配
     * （用 `-`/`.` 作边界，避免 32 位 x86 设备误匹配到 x86_64 包），
     * 其次退回 universal 包；都不匹配时返回 null，让界面回退到浏览器由用户自选。
     */
    internal fun selectApkUrl(assets: List<Pair<String, String>>, supportedAbis: List<String>): String? {
        val apks = assets.filter { (name, url) -> name.endsWith(".apk", ignoreCase = true) && url.isNotBlank() }
        if (apks.isEmpty()) return null
        supportedAbis.forEach { abi ->
            val pattern = abiPattern(abi)
            apks.firstOrNull { (name, _) -> pattern.containsMatchIn(name) }?.let { return it.second }
        }
        apks.firstOrNull { (name, _) -> name.contains("universal", ignoreCase = true) }?.let { return it.second }
        // 没有匹配到架构时，只有「名字里不带任何架构标记」的唯一 APK 才可以直接用；
        // 否则宁可回退到浏览器，也不要把别的架构装到用户机器上。
        val only = apks.singleOrNull() ?: return null
        if (KNOWN_ABIS.any { abiPattern(it).containsMatchIn(only.first) }) return null
        return only.second
    }

    private fun abiPattern(abi: String) =
        Regex("(^|[-.])${Regex.escape(abi)}([-.]|$)", RegexOption.IGNORE_CASE)

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
        val KNOWN_ABIS = listOf("arm64-v8a", "armeabi-v7a", "armeabi", "x86_64", "x86", "mips64", "mips")

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
