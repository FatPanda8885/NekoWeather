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
}
