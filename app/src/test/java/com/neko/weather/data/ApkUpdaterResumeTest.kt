package com.neko.weather.data

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

class ApkUpdaterResumeTest {
    private val payload = ByteArray(200_000) { (it % 251).toByte() }

    /** 断流后必须留下 .meta（含 ETag），否则下次无法构造 If-Range，续传退化成重下。 */
    @Test
    fun keepsResumeMetadataWhenTheStreamIsInterrupted() {
        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = MockResponse()
                .addHeader("ETag", "\"etag-1\"")
                .setBody(Buffer().write(payload))
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY)
        }
        server.start()
        val dir = Files.createTempDirectory("nekoweather-apk").toFile()
        try {
            val url = server.url("/NekoWeather.apk").toString()
            val first = runCatching { ApkUpdater.downloadInto(dir, url, "NekoWeather.apk") {} }

            assertTrue("断流时首次下载应当失败", first.isFailure)
            val meta = File(dir, "NekoWeather.apk.meta")
            assertTrue("断流后必须留下 .meta", meta.exists())
            assertTrue("meta 里必须记住 ETag", meta.readText().contains("etag-1"))
        } finally {
            server.shutdown()
            dir.deleteRecursively()
        }
    }

    /** 有半成品 + ETag 时，重试必须带 Range 续传，并且拼出完整文件。 */
    @Test
    fun resumesFromThePartialFileWithRange() {
        val server = MockWebServer()
        var rangeHeader: String? = null
        var ifRangeHeader: String? = null
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                rangeHeader = request.getHeader("Range")
                ifRangeHeader = request.getHeader("If-Range")
                val from = rangeHeader?.removePrefix("bytes=")?.substringBefore('-')?.toIntOrNull() ?: 0
                return MockResponse()
                    .setResponseCode(206)
                    .addHeader("ETag", "\"etag-1\"")
                    .addHeader("Content-Range", "bytes $from-${payload.size - 1}/${payload.size}")
                    .setBody(Buffer().write(payload, from, payload.size - from))
            }
        }
        server.start()
        val dir = Files.createTempDirectory("nekoweather-apk").toFile()
        try {
            val url = server.url("/NekoWeather.apk").toString()
            val half = payload.size / 2
            File(dir, "NekoWeather.apk.part").writeBytes(payload.copyOfRange(0, half))
            File(dir, "NekoWeather.apk.meta").writeText(
                """{"url":"$url","etag":"\"etag-1\"","total":${payload.size}}""",
            )

            val file = ApkUpdater.downloadInto(dir, url, "NekoWeather.apk") {}

            assertTrue("必须发 Range 续传", rangeHeader?.startsWith("bytes=") == true)
            assertTrue("必须发 If-Range 校验资源未变", ifRangeHeader?.contains("etag-1") == true)
            assertArrayEquals(payload, file.readBytes())
        } finally {
            server.shutdown()
            dir.deleteRecursively()
        }
    }
}
