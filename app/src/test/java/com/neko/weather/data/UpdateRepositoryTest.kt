package com.neko.weather.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRepositoryTest {
    private val repository = UpdateRepository()

    @Test
    fun comparesSemanticVersionNumbers() {
        assertTrue(repository.compareVersions("1.10.0", "1.9.9") > 0)
        assertTrue(repository.compareVersions("2.0.0-beta", "1.9.9") > 0)
        assertEquals(0, repository.compareVersions("v1.2", "1.2.0"))
    }

    @Test
    fun selectsGiteeForMainlandChina() {
        val source = repository.updateSourceFor(mainland = true)

        assertEquals("Gitee", source.name)
        assertEquals("https://gitee.com/api/v5/repos/pasky/NekoWeather/releases/latest", source.latestReleaseApi)
        assertEquals("https://gitee.com/pasky/NekoWeather/releases", source.releasePage)
    }

    @Test
    fun selectsGitHubOutsideMainlandChina() {
        val source = repository.updateSourceFor(mainland = false)

        assertEquals("GitHub", source.name)
        assertEquals("https://api.github.com/repos/FatPanda8885/NekoWeather/releases/latest", source.latestReleaseApi)
    }

    private fun assets(vararg names: String) = names.map { it to "https://example.test/$it" }

    @Test
    fun picksTheAbiMatchingTheDevice() {
        val assets = assets(
            "NekoWeather-1.1.0-arm64-v8a.apk",
            "NekoWeather-1.1.0-x86_64.apk",
            "NekoWeather-1.1.0-universal.apk",
        )

        assertEquals(
            "https://example.test/NekoWeather-1.1.0-arm64-v8a.apk",
            repository.selectApkUrl(assets, listOf("arm64-v8a", "armeabi-v7a")),
        )
        assertEquals(
            "https://example.test/NekoWeather-1.1.0-x86_64.apk",
            repository.selectApkUrl(assets, listOf("x86_64", "x86")),
        )
    }

    @Test
    fun doesNotMistakeX86_64ForX86() {
        val assets = assets("NekoWeather-1.1.0-x86_64.apk", "NekoWeather-1.1.0-arm64-v8a.apk")

        // 只有 32 位 x86 的设备不该拿到 x86_64 包，也不该乱退回别的架构
        assertEquals("https://example.test/NekoWeather-1.1.0-arm64-v8a.apk", repository.selectApkUrl(assets, listOf("arm64-v8a")))
        assertEquals(null, repository.selectApkUrl(assets - assets.first(), listOf("x86")))
    }

    @Test
    fun fallsBackToUniversalThenBrowser() {
        val withUniversal = assets(
            "NekoWeather-1.1.0-armeabi-v7a.apk",
            "NekoWeather-1.1.0-universal.apk",
        )
        assertEquals(
            "https://example.test/NekoWeather-1.1.0-universal.apk",
            repository.selectApkUrl(withUniversal, listOf("mips64")),
        )

        val withoutUniversal = assets("NekoWeather-1.1.0-armeabi-v7a.apk")
        assertEquals(null, repository.selectApkUrl(withoutUniversal, listOf("mips64")))
    }

    @Test
    fun acceptsASingleApkWithoutAbiSuffix() {
        assertEquals(
            "https://example.test/NekoWeather.apk",
            repository.selectApkUrl(assets("NekoWeather.apk"), listOf("arm64-v8a")),
        )
    }

    @Test
    fun ignoresNonApkAssets() {
        val mixed = listOf(
            "NekoWeather-1.1.0.apk.sha256" to "https://example.test/digest",
            "notes.txt" to "https://example.test/notes",
            "NekoWeather-1.1.0-universal.apk" to "https://example.test/universal",
        )
        assertEquals("https://example.test/universal", repository.selectApkUrl(mixed, listOf("arm64-v8a")))
    }
}
